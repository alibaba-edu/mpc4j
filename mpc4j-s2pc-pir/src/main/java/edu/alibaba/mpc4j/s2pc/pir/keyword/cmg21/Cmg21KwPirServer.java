package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipher;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerNode;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerUtils;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CMG21 keyword PIR server.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirServer extends AbstractKwPirServer {
    /**
     * stream cipher
     */
    private final StreamCipher streamCipher;
    /**
     * compress encode
     */
    private final boolean compressEncode;
    /**
     * CMG21 keyword PIR params
     */
    private Cmg21KwPirParams params;
    /**
     * hash bins
     */
    private List<List<HashBinEntry<ByteBuffer>>> hashBins;
    /**
     * server encoded keyword
     */
    private long[][][] serverKeywordEncode;
    /**
     * server encoded label
     */
    private long[][][] serverLabelEncode;
    /**
     * PRF key
     */
    private BigInteger alpha;
    /**
     * keyword response
     */
    private List<byte[]> keywordResponsePayload;
    /**
     * label response
     */
    private List<byte[]> labelResponsePayload;
    /**
     * hash keys
     */
    private byte[][] hashKeys;

    public Cmg21KwPirServer(Rpc serverRpc, Party clientParty, Cmg21KwPirConfig config) {
        super(Cmg21KwPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        compressEncode = config.getCompressEncode();
        streamCipher = StreamCipherFactory.createInstance(envType);
    }

    @Override
    public void init(KwPirParams kwPirParams, Map<ByteBuffer, ByteBuffer> serverKeywordLabelMap, int labelByteLength) {
        setInitInput(serverKeywordLabelMap, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        assert (kwPirParams instanceof Cmg21KwPirParams);
        params = (Cmg21KwPirParams) kwPirParams;
        hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, cuckooHashKeyTime, "Server generates cuckoo hash keys");

        stopWatch.start();
        // generate prf
        List<ByteBuffer> keywordPrfs = computeKeywordPrf();
        Map<ByteBuffer, ByteBuffer> prfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(
                Collectors.toMap(keywordPrfs::get, i -> serverKeywordLabelMap.get(keywordList.get(i)), (a, b) -> b)
            );
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, oprfTime, "Server computes PRFs");

        stopWatch.start();
        // generate complete hash bin
        hashBins = generateCompleteHashBin(keywordPrfs, params.getBinNum());
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, hashTime, "Server bin-hashes key");

        stopWatch.start();
        // encode database
        encodeDatabase(prfLabelMap);
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, encodeTime, "Server encodes label");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(Map<ByteBuffer, ByteBuffer> serverKeywordLabelMap, int maxRetrievalSize, int labelByteLength)
        throws MpcAbortException {
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        if (maxRetrievalSize > 1) {
            params = Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096;
        } else {
            params = Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1;
        }
        setInitInput(serverKeywordLabelMap, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, cuckooHashKeyTime, "Server generates cuckoo hash keys");

        stopWatch.start();
        // compute prf
        List<ByteBuffer> keywordPrfs = computeKeywordPrf();
        Map<ByteBuffer, ByteBuffer> prfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(
                Collectors.toMap(keywordPrfs::get, i -> serverKeywordLabelMap.get(keywordList.get(i)), (a, b) -> b)
            );
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, oprfTime, "Server computes PRFs");

        stopWatch.start();
        // generate complete hash bins
        hashBins = generateCompleteHashBin(keywordPrfs, params.getBinNum());
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, hashTime, "Server bin-hashes key");

        stopWatch.start();
        // encode database
        encodeDatabase(prfLabelMap);
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, encodeTime, "Server encodes label");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // OPRF
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();
        List<byte[]> blindPrfPayload = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrfPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Server runs OPRFs");

        DataPacketHeader encryptionParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_FHE_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> encryptionParamsPayload = rpc.receive(encryptionParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            encryptionParamsPayload.size() == 3, "Failed to receive BFV encryption parameters"
        );
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();
        int ciphertextNumber = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == ciphertextNumber * params.getQueryPowers().length,
            "The size of query is incorrect"
        );

        stopWatch.start();
        computeResponse(queryPayload, encryptionParamsPayload);
        DataPacketHeader keywordResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keywordResponseHeader, keywordResponsePayload));
        DataPacketHeader labelResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(labelResponseHeader, labelResponsePayload));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * sort hash bin items.
     *
     * @param binItems bin items.
     * @return sorted items.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<HashBinEntry<ByteBuffer>> sortedHashBinEntries(List<HashBinEntry<ByteBuffer>> binItems)
        throws MpcAbortException {
        List<List<HashBinEntry<ByteBuffer>>> partitions = new ArrayList<>(binItems.size());
        List<List<Set<Long>>> partElementSets = new ArrayList<>();
        for (int i = 0; i < binItems.size(); i++) {
            partElementSets.add(i, new ArrayList<>());
            for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                partElementSets.get(i).add(j, new HashSet<>());
            }
        }
        for (int i = 0; i < binItems.size(); i++) {
            partitions.add(new ArrayList<>());
        }
        BigInteger blockMask = BigInteger.ONE.shiftLeft(CommonConstants.BLOCK_BIT_LENGTH).subtract(BigInteger.ONE);
        int shiftBits = BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1;
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(shiftBits).subtract(BigInteger.ONE);
        for (HashBinEntry<ByteBuffer> binItem : binItems) {
            long[] itemParts = new long[params.getItemEncodedSlotSize()];
            BigInteger item = BigIntegerUtils.byteArrayToBigInteger(binItem.getItem().array());
            item = item.and(blockMask);
            for (int i = 0; i < params.getItemEncodedSlotSize(); i++) {
                itemParts[i] = item.and(shiftMask).longValueExact();
                item = item.shiftRight(shiftBits);
            }
            for (int i = 0; i < partitions.size(); i++) {
                List<HashBinEntry<ByteBuffer>> partition = partitions.get(i);
                if (partition.size() == 0) {
                    partition.add(binItem);
                    for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                        partElementSets.get(i).get(j).add(itemParts[j]);
                    }
                    break;
                } else {
                    if (partition.size() != params.getMaxPartitionSizePerBin()) {
                        int l;
                        for (l = 0; l < partition.size(); l++) {
                            if (checkRepeatedItemPart(partElementSets.get(i), itemParts)) {
                                break;
                            }
                        }
                        if (l == partition.size()) {
                            partition.add(binItem);
                            for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                                partElementSets.get(i).get(j).add(itemParts[j]);
                            }
                            break;
                        }
                    }
                }
            }
        }
        Stream<List<HashBinEntry<ByteBuffer>>> partitionStream = partitions.stream();
        partitionStream = parallel ? partitionStream.parallel() : partitionStream;
        return partitionStream
            .filter(partition -> partition.size() != 0)
            .peek(partition -> {
                for (int index = partition.size(); index < params.getMaxPartitionSizePerBin(); index++) {
                    partition.add(HashBinEntry.fromEmptyItem(botElementByteBuffer));
                }
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * generate complete hash bins.
     *
     * @param itemList item list.
     * @param binNum   bin num.
     * @return complete hash bins.
     */
    private List<List<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(List<ByteBuffer> itemList, int binNum) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, keywordSize, hashKeys);
        completeHash.insertItems(itemList);
        IntStream binIndexIntStream = IntStream.range(0, completeHash.binNum());
        binIndexIntStream = parallel ? binIndexIntStream.parallel() : binIndexIntStream;
        List<List<HashBinEntry<ByteBuffer>>> hashBinList = binIndexIntStream
            .mapToObj(binIndex -> {
                List<HashBinEntry<ByteBuffer>> binList = new ArrayList<>(completeHash.getBin(binIndex));
                try {
                    return sortedHashBinEntries(binList);
                } catch (MpcAbortException e) {
                    e.printStackTrace();
                }
                return binList;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        int maxBinSize = hashBinList.stream().mapToInt(List::size).max().orElse(0);
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botElementByteBuffer);
        hashBinList.forEach(bin -> {
            int paddingNum = maxBinSize - bin.size();
            for (int index = 0; index < paddingNum; index++) {
                bin.add(paddingEntry);
            }
        });
        return hashBinList;
    }

    /**
     * check repeated item part.
     *
     * @param existingItemParts existing item part.
     * @param itemParts         item parts.
     * @return whether existing list has the input item.
     */
    private boolean checkRepeatedItemPart(List<Set<Long>> existingItemParts, long[] itemParts) {
        assert existingItemParts.size() == itemParts.length;
        return IntStream.range(0, itemParts.length).anyMatch(i -> existingItemParts.get(i).contains(itemParts[i]));
    }

    /**
     * encode database.
     *
     * @param prfMap prf map.
     */
    private void encodeDatabase(Map<ByteBuffer, ByteBuffer> prfMap) {
        long plainModulus = params.getPlainModulus();
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, plainModulus);
        int itemEncodedSlotSize = params.getItemEncodedSlotSize();
        int itemPerCiphertext = params.getPolyModulusDegree() / itemEncodedSlotSize;
        int ciphertextNum = params.getBinNum() / itemPerCiphertext;
        int binSize = hashBins.get(0).size();
        int partitionCount = CommonUtils.getUnitNum(binSize, params.getMaxPartitionSizePerBin());
        int bigPartitionCount = binSize / params.getMaxPartitionSizePerBin();
        int labelPartitionCount = CommonUtils.getUnitNum((labelByteLength+CommonConstants.BLOCK_BYTE_LENGTH)*Byte.SIZE,
            ((LongUtils.ceilLog2(plainModulus) - 1) * itemEncodedSlotSize));
        serverKeywordEncode = new long[partitionCount * ciphertextNum][][];
        serverLabelEncode = new long[partitionCount * ciphertextNum * labelPartitionCount][][];
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        // and coeffs of g(x), which has the property g(y) = label(y) for each y in bucket.
        // ciphertext num is small, therefore we need to do parallel computation inside the loop
        for (int i = 0; i < ciphertextNum; i++) {
            int finalIndex = i;
            for (int partition = 0; partition < partitionCount; partition++) {
                // keyword coeffs
                long[][] fCoeffs = new long[itemPerCiphertext * itemEncodedSlotSize][];
                // label coeffs
                long[][][] gCoeffs = new long[labelPartitionCount][itemPerCiphertext * itemEncodedSlotSize][];
                int partitionSize, partitionStart;
                partitionSize = partition < bigPartitionCount ?
                    params.getMaxPartitionSizePerBin() : binSize % params.getMaxPartitionSizePerBin();
                partitionStart = params.getMaxPartitionSizePerBin() * partition;
                IntStream itemIndexStream = IntStream.range(0, itemPerCiphertext);
                itemIndexStream = parallel ? itemIndexStream.parallel() : itemIndexStream;
                itemIndexStream.forEach(j -> {
                    long[][] currentBucketElement = new long[itemEncodedSlotSize][partitionSize];
                    long[][][] currentBucketLabels = new long[labelPartitionCount][itemEncodedSlotSize][partitionSize];
                    for (int l = 0; l < partitionSize; l++) {
                        HashBinEntry<ByteBuffer> entry = hashBins.get(
                            finalIndex * itemPerCiphertext + j).get(partitionStart + l
                        );
                        long[] temp = params.getHashBinEntryEncodedArray(entry, false, secureRandom);
                        for (int k = 0; k < itemEncodedSlotSize; k++) {
                            currentBucketElement[k][l] = temp[k];
                        }
                    }
                    for (int l = 0; l < itemEncodedSlotSize; l++) {
                        fCoeffs[j * itemEncodedSlotSize + l] = zp64Poly.rootInterpolate(
                            partitionSize, currentBucketElement[l], 0L
                        );
                    }
                    int nonEmptyBuckets = 0;
                    for (int l = 0; l < partitionSize; l++) {
                        HashBinEntry<ByteBuffer> entry = hashBins.get(
                            finalIndex * itemPerCiphertext + j).get(partitionStart + l
                        );
                        if (entry.getHashIndex() != HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                            for (int k = 0; k < itemEncodedSlotSize; k++) {
                                currentBucketElement[k][nonEmptyBuckets] = currentBucketElement[k][l];
                            }
                            byte[] oprf = entry.getItem().array();
                            // choose first 128 bits
                            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            System.arraycopy(oprf, 0, keyBytes, 0, CommonConstants.BLOCK_BYTE_LENGTH);
                            byte[] iv = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            secureRandom.nextBytes(iv);
                            byte[] plaintextLabel = prfMap.get(entry.getItem()).array();
                            byte[] ciphertextLabel = streamCipher.ivEncrypt(keyBytes, iv, plaintextLabel);
                            long[][] temp = params.encodeLabel(ciphertextLabel, labelPartitionCount);
                            for (int k = 0; k < labelPartitionCount; k++) {
                                for (int h = 0; h < itemEncodedSlotSize; h++) {
                                    currentBucketLabels[k][h][nonEmptyBuckets] = temp[k][h];
                                }
                            }
                            nonEmptyBuckets++;
                        }
                    }
                    for (int l = 0; l < itemEncodedSlotSize; l++) {
                        for (int k = 0; k < labelPartitionCount; k++) {
                            long[] xArray = new long[nonEmptyBuckets];
                            long[] yArray = new long[nonEmptyBuckets];
                            for (int index = 0; index < nonEmptyBuckets; index++) {
                                xArray[index] = currentBucketElement[l][index];
                                yArray[index] = currentBucketLabels[k][l][index];
                            }
                            if (nonEmptyBuckets > 0) {
                                gCoeffs[k][j * itemEncodedSlotSize + l] = zp64Poly.interpolate(
                                    nonEmptyBuckets, xArray, yArray
                                );
                            } else {
                                gCoeffs[k][j * itemEncodedSlotSize + l] = new long[0];
                            }
                        }
                    }
                });
                long[][] encodeElementVector = new long[partitionSize + 1][params.getPolyModulusDegree()];
                int labelSize = IntStream.range(1, gCoeffs[0].length)
                    .map(j -> gCoeffs[0][j].length).filter(j -> j >= 1)
                    .max()
                    .orElse(1);
                long[][][] encodeLabelVector = new long[labelPartitionCount][labelSize][params.getPolyModulusDegree()];
                for (int j = 0; j < partitionSize + 1; j++) {
                    // encode the jth coefficients of all polynomials into a vector
                    for (int l = 0; l < itemEncodedSlotSize * itemPerCiphertext; l++) {
                        encodeElementVector[j][l] = fCoeffs[l][j];
                    }
                    for (int l = itemEncodedSlotSize * itemPerCiphertext; l < params.getPolyModulusDegree(); l++) {
                        encodeElementVector[j][l] = 0;
                    }
                }
                for (int j = 0; j < labelSize; j++) {
                    for (int k = 0; k < labelPartitionCount; k++) {
                        for (int l = 0; l < itemEncodedSlotSize * itemPerCiphertext; l++) {
                            if (gCoeffs[k][l].length == 0) {
                                encodeLabelVector[k][j][l] = Math.abs(secureRandom.nextLong()) % plainModulus;
                            } else {
                                encodeLabelVector[k][j][l] = (j < gCoeffs[k][l].length) ? gCoeffs[k][l][j] : 0;
                            }
                        }
                        for (int l = itemEncodedSlotSize * itemPerCiphertext; l < params.getPolyModulusDegree(); l++) {
                            encodeLabelVector[k][j][l] = 0;
                        }
                    }
                }
                serverKeywordEncode[partition + i * partitionCount] = encodeElementVector;
                for (int j = 0; j < labelPartitionCount; j++) {
                    serverLabelEncode[j + partition * labelPartitionCount + i * partitionCount * labelPartitionCount] =
                        encodeLabelVector[j];
                }
            }
        }
    }

    /**
     * handle blind element.
     *
     * @param blindElements blind element.
     * @return blind element prf.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> handleBlindPayload(List<byte[]> blindElements) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindElements.size() > 0);
        Ecc ecc = EccFactory.createInstance(envType);
        Stream<byte[]> blindStream = blindElements.stream();
        blindStream = parallel ? blindStream.parallel() : blindStream;
        return blindStream
            // decode H(m_c)^β
            .map(ecc::decode)
            // compute H(m_c)^βα
            .map(element -> ecc.multiply(element, alpha))
            // encode
            .map(element -> ecc.encode(element, compressEncode))
            .collect(Collectors.toList());
    }

    /**
     * generate response.
     *
     * @param encryptedQuery   client query.
     * @param encryptionParams encryption params and public keys.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void computeResponse(List<byte[]> encryptedQuery, List<byte[]> encryptionParams) throws MpcAbortException {
        int binSize = hashBins.get(0).size();
        int partitionCount = CommonUtils.getUnitNum(binSize, params.getMaxPartitionSizePerBin());
        int[][] powerDegree;
        if (params.getPsLowDegree() > 0) {
            Set<Integer> innerPowersSet = new HashSet<>();
            Set<Integer> outerPowersSet = new HashSet<>();
            IntStream.range(0, params.getQueryPowers().length).forEach(i -> {
                if (params.getQueryPowers()[i] <= params.getPsLowDegree()) {
                    innerPowersSet.add(params.getQueryPowers()[i]);
                } else {
                    outerPowersSet.add(params.getQueryPowers()[i] / (params.getPsLowDegree() + 1));
                }
            });
            PowerNode[] innerPowerNodes = PowerUtils.computePowers(innerPowersSet, params.getPsLowDegree());
            PowerNode[] outerPowerNodes = PowerUtils.computePowers(
                outerPowersSet, params.getMaxPartitionSizePerBin() / (params.getPsLowDegree() + 1));
            powerDegree = new int[innerPowerNodes.length + outerPowerNodes.length][2];
            int[][] innerPowerNodesDegree = Arrays.stream(innerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            int[][] outerPowerNodesDegree = Arrays.stream(outerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            System.arraycopy(innerPowerNodesDegree, 0, powerDegree, 0, innerPowerNodesDegree.length);
            System.arraycopy(outerPowerNodesDegree, 0, powerDegree, innerPowerNodesDegree.length, outerPowerNodesDegree.length);
        } else {
            Set<Integer> sourcePowersSet = Arrays.stream(params.getQueryPowers())
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
            PowerNode[] powerNodes = PowerUtils.computePowers(sourcePowersSet, params.getMaxPartitionSizePerBin());
            powerDegree = Arrays.stream(powerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
        }
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int labelPartitionCount = CommonUtils.getUnitNum((labelByteLength+CommonConstants.BLOCK_BYTE_LENGTH)*Byte.SIZE,
            (BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1) * params.getItemEncodedSlotSize());
        IntStream queryIntStream = parallel ?
            IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum);
        List<byte[]> queryPowers = queryIntStream
            .mapToObj(i -> Cmg21KwPirNativeUtils.computeEncryptedPowers(
                encryptionParams.get(0),
                encryptionParams.get(1),
                encryptedQuery.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree()))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.getPsLowDegree() > 0) {
            keywordResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.optComputeMatches(
                                encryptionParams.get(0),
                                encryptionParams.get(2),
                                encryptionParams.get(1),
                                serverKeywordEncode[i * partitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            labelResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.optComputeMatches(
                                encryptionParams.get(0),
                                encryptionParams.get(2),
                                encryptionParams.get(1),
                                serverLabelEncode[i * partitionCount * labelPartitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        } else if (params.getPsLowDegree() == 0) {
            keywordResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.naiveComputeMatches(
                                encryptionParams.get(0),
                                encryptionParams.get(2),
                                serverKeywordEncode[i * partitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            labelResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.naiveComputeMatches(
                                encryptionParams.get(0),
                                encryptionParams.get(2),
                                serverLabelEncode[i * partitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        } else {
            throw new MpcAbortException("ps_low_degree is incorrect.");
        }
    }

    /**
     * compute keyword prf.
     *
     * @return keyword prf.
     */
    private List<ByteBuffer> computeKeywordPrf() {
        Ecc ecc = EccFactory.createInstance(envType);
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        Stream<ByteBuffer> keywordStream = keywordList.stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        return keywordStream
            .map(keyword -> ecc.hashToCurve(keyword.array()))
            .map(hash -> ecc.multiply(hash, alpha))
            .map(prf -> ecc.encode(prf, false))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}