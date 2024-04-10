package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipher;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowersDag;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirPtoDesc.PtoStep;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

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
     * CMG21 keyword PIR params
     */
    private Cmg21KwPirParams params;
    /**
     * server encoded keyword
     */
    private List<List<byte[]>> serverKeywordEncode;
    /**
     * server encoded label
     */
    private List<List<byte[]>> serverLabelEncode;
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
     * ecc
     */
    private final ByteFullEcc ecc;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * relinearization keys
     */
    private byte[] relinKeys;
    /**
     * iv byte length
     */
    private final int ivByteLength;
    /**
     * bin size
     */
    private int binSize;

    public Cmg21KwPirServer(Rpc serverRpc, Party clientParty, Cmg21KwPirConfig config) {
        super(Cmg21KwPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
        streamCipher = StreamCipherFactory.createInstance(envType);
        ivByteLength = 0;
    }

    @Override
    public void init(KwPirParams kwPirParams, Map<ByteBuffer, byte[]> keywordLabelMap, int maxRetrievalSize,
                     int labelByteLength) throws MpcAbortException {
        setInitInput(keywordLabelMap, maxRetrievalSize, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        assert (kwPirParams instanceof Cmg21KwPirParams);
        params = (Cmg21KwPirParams) kwPirParams;
        assert maxRetrievalSize <= params.maxRetrievalSize();

        DataPacketHeader clientPublicKeysPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeysPayload = rpc.receive(clientPublicKeysPayloadHeader).getPayload();
        MpcAbortPreconditions.checkArgument(publicKeysPayload.size() == 2, "Failed to receive BFV public keys payload");
        this.publicKey = publicKeysPayload.remove(0);
        this.relinKeys = publicKeysPayload.remove(0);

        stopWatch.start();
        // generate prf
        List<ByteBuffer> keywordPrfs = computeKeywordPrf();
        Map<ByteBuffer, byte[]> prfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(Collectors.toMap(keywordPrfs::get, i -> keywordLabelMap.get(keywordList.get(i)), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, oprfTime, "Server computes PRFs");

        stopWatch.start();
        // generate hash bins
        byte[][] hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
        List<List<HashBinEntry<ByteBuffer>>> hashBins = generateCompleteHashBin(keywordPrfs, params.getBinNum(), hashKeys);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, hashTime, "Server generates hash bins");

        stopWatch.start();
        // encode database
        encodeDatabase(prfLabelMap, hashBins);
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, encodeTime, "Server encodes label");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(Map<ByteBuffer, byte[]> keywordLabelMap, int maxRetrievalSize, int labelByteLength)
        throws MpcAbortException {
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        if (maxRetrievalSize > 1) {
            params = Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096;
        } else {
            if (keywordLabelMap.size() <= (1 << 20)) {
                params = Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1;
            } else {
                params = Cmg21KwPirParams.SERVER_16M_CLIENT_MAX_1;
            }
        }
        setInitInput(keywordLabelMap, maxRetrievalSize, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        assert maxRetrievalSize <= params.maxRetrievalSize();

        DataPacketHeader clientPublicKeysPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeysPayload = rpc.receive(clientPublicKeysPayloadHeader).getPayload();
        MpcAbortPreconditions.checkArgument(publicKeysPayload.size() == 2, "Failed to receive BFV public keys payload");
        this.publicKey = publicKeysPayload.remove(0);
        this.relinKeys = publicKeysPayload.remove(0);

        stopWatch.start();
        // generate prf
        List<ByteBuffer> keywordPrfs = computeKeywordPrf();
        Map<ByteBuffer, byte[]> prfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(Collectors.toMap(keywordPrfs::get, i -> keywordLabelMap.get(keywordList.get(i)), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, oprfTime, "Server computes PRFs");

        stopWatch.start();
        // generate hash bins
        byte[][] hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
        List<List<HashBinEntry<ByteBuffer>>> hashBins = generateCompleteHashBin(keywordPrfs, params.getBinNum(), hashKeys);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, hashTime, "Server generates hash bins");

        stopWatch.start();
        // encode database
        encodeDatabase(prfLabelMap, hashBins);
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, encodeTime, "Server encodes label");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // OPRF
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();

        stopWatch.start();
        List<byte[]> blindPrfPayload = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrfPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Server executes OPRF");

        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == params.getCiphertextNum() * params.getQueryPowers().length,
            "The size of query is incorrect"
        );

        stopWatch.start();
        computeResponse(queryPayload);
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
     */
    private List<HashBinEntry<ByteBuffer>> sortedHashBinEntries(List<HashBinEntry<ByteBuffer>> binItems) {
        List<List<Set<Long>>> partElementSets = new ArrayList<>();
        for (int i = 0; i < binItems.size(); i++) {
            partElementSets.add(i, new ArrayList<>());
            for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                partElementSets.get(i).add(j, new HashSet<>());
            }
        }
        List<List<HashBinEntry<ByteBuffer>>> partitions = IntStream.range(0, binItems.size())
            .<List<HashBinEntry<ByteBuffer>>>mapToObj(i -> new ArrayList<>())
            .collect(Collectors.toCollection(() -> new ArrayList<>(binItems.size())));
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
                        if (!checkRepeatedItemPart(partElementSets.get(i), itemParts)) {
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
     * @param hashKeys hash keys.
     * @return complete hash bins.
     */
    private List<List<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(List<ByteBuffer> itemList, int binNum,
                                                                         byte[][] hashKeys) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, keywordSize, hashKeys);
        completeHash.insertItems(itemList);
        IntStream intStream = IntStream.range(0, binNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<List<HashBinEntry<ByteBuffer>>> hashBinList = intStream
            .mapToObj(binIndex -> sortedHashBinEntries(new ArrayList<>(completeHash.getBin(binIndex))))
            .collect(Collectors.toCollection(ArrayList::new));
        binSize = hashBinList.stream().mapToInt(List::size).max().orElse(0);
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botElementByteBuffer);
        hashBinList.forEach(bin -> {
            int paddingNum = binSize - bin.size();
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
    private void encodeDatabase(Map<ByteBuffer, byte[]> prfMap, List<List<HashBinEntry<ByteBuffer>>> hashBins) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, params.getPlainModulus());
        int itemPerCiphertext = params.getItemPerCiphertext();
        int itemEncodedSlotSize = params.getItemEncodedSlotSize();
        int partitionCount = CommonUtils.getUnitNum(binSize, params.getMaxPartitionSizePerBin());
        int bigPartitionCount = binSize / params.getMaxPartitionSizePerBin();
        int labelPartitionCount = CommonUtils.getUnitNum((labelByteLength + ivByteLength) * Byte.SIZE,
            (PirUtils.getBitLength(params.getPlainModulus()) - 1) * itemEncodedSlotSize);
        serverKeywordEncode = new ArrayList<>();
        serverLabelEncode = new ArrayList<>();
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        // and coeffs of g(x), which has the property g(y) = label(y) for each y in bucket.
        // ciphertext num is small, therefore we need to do parallel computation inside the loop
        for (int i = 0; i < params.getCiphertextNum(); i++) {
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
                            byte[] iv = new byte[ivByteLength];
                            secureRandom.nextBytes(iv);
                            byte[] extendedIv = BytesUtils.paddingByteArray(iv, CommonConstants.BLOCK_BYTE_LENGTH);
                            byte[] plaintextLabel = prfMap.get(entry.getItem());
                            byte[] extendedCipherLabel = streamCipher.ivEncrypt(keyBytes, extendedIv, plaintextLabel);
                            byte[] ciphertextLabel = new byte[ivByteLength + labelByteLength];
                            System.arraycopy(extendedCipherLabel, CommonConstants.BLOCK_BYTE_LENGTH - ivByteLength, ciphertextLabel, 0, ivByteLength + labelByteLength);
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
                                encodeLabelVector[k][j][l] = Math.abs(secureRandom.nextLong()) % params.getPlainModulus();
                            } else {
                                encodeLabelVector[k][j][l] = (j < gCoeffs[k][l].length) ? gCoeffs[k][l][j] : 0;
                            }
                        }
                        for (int l = itemEncodedSlotSize * itemPerCiphertext; l < params.getPolyModulusDegree(); l++) {
                            encodeLabelVector[k][j][l] = 0;
                        }
                    }
                }
                serverKeywordEncode.add(Cmg21KwPirNativeUtils.preprocessDatabase(
                    params.getEncryptionParams(), encodeElementVector, params.getPsLowDegree())
                );
                for (int j = 0; j < labelPartitionCount; j++) {
                    serverLabelEncode.add(Cmg21KwPirNativeUtils.preprocessDatabase(
                        params.getEncryptionParams(), encodeLabelVector[j], params.getPsLowDegree()
                    ));
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
        Stream<byte[]> blindStream = blindElements.stream();
        blindStream = parallel ? blindStream.parallel() : blindStream;
        return blindStream
            // compute H(m_c)^βα
            .map(element -> ecc.mul(element, alpha))
            .collect(Collectors.toList());
    }

    /**
     * generate response.
     *
     * @param encryptedQuery client query.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void computeResponse(List<byte[]> encryptedQuery) throws MpcAbortException {
        int partitionCount = CommonUtils.getUnitNum(binSize, params.getMaxPartitionSizePerBin());
        int[][] powerDegree;
        if (params.getPsLowDegree() > 0) {
            int queryPowerNum = params.getQueryPowers().length;
            TIntSet innerPowersSet = new TIntHashSet(queryPowerNum);
            TIntSet outerPowersSet = new TIntHashSet(queryPowerNum);
            for (int i = 0; i < queryPowerNum; i++) {
                if (params.getQueryPowers()[i] <= params.getPsLowDegree()) {
                    innerPowersSet.add(params.getQueryPowers()[i]);
                } else {
                    outerPowersSet.add(params.getQueryPowers()[i] / (params.getPsLowDegree() + 1));
                }
            }
            PowersDag innerPowersDag = new PowersDag(innerPowersSet, params.getPsLowDegree());
            PowersDag outerPowersDag = new PowersDag(
                outerPowersSet, params.getMaxPartitionSizePerBin() / (params.getPsLowDegree() + 1)
            );
            powerDegree = new int[innerPowersDag.upperBound() + outerPowersDag.upperBound()][2];
            int[][] innerPowerNodesDegree = innerPowersDag.getDag();
            int[][] outerPowerNodesDegree = outerPowersDag.getDag();
            System.arraycopy(innerPowerNodesDegree, 0, powerDegree, 0, innerPowerNodesDegree.length);
            System.arraycopy(outerPowerNodesDegree, 0, powerDegree, innerPowerNodesDegree.length, outerPowerNodesDegree.length);
        } else {
            TIntSet sourcePowersSet = new TIntHashSet(params.getQueryPowers());
            PowersDag powersDag = new PowersDag(sourcePowersSet, params.getMaxPartitionSizePerBin());
            powerDegree = powersDag.getDag();
        }
        int labelPartitionCount = CommonUtils.getUnitNum((labelByteLength + ivByteLength) * Byte.SIZE,
            (PirUtils.getBitLength(params.getPlainModulus()) - 1) * params.getItemEncodedSlotSize());
        IntStream queryIntStream = parallel ?
            IntStream.range(0, params.getCiphertextNum()).parallel() : IntStream.range(0, params.getCiphertextNum());
        List<byte[]> queryPowers = queryIntStream
            .mapToObj(i -> Cmg21KwPirNativeUtils.computeEncryptedPowers(
                params.getEncryptionParams(),
                relinKeys,
                encryptedQuery.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree()))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.getPsLowDegree() > 0) {
            keywordResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.optComputeMatches(
                                params.getEncryptionParams(),
                                publicKey,
                                relinKeys,
                                serverKeywordEncode.get(i * partitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            labelResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.optComputeMatches(
                                params.getEncryptionParams(),
                                publicKey,
                                relinKeys,
                                serverLabelEncode.get(i * partitionCount * labelPartitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        } else if (params.getPsLowDegree() == 0) {
            keywordResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.naiveComputeMatches(
                                params.getEncryptionParams(),
                                publicKey,
                                serverKeywordEncode.get(i * partitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            labelResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.naiveComputeMatches(
                                params.getEncryptionParams(),
                                publicKey,
                                serverLabelEncode.get(i * partitionCount + j),
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
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        Stream<ByteBuffer> keywordStream = keywordList.stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        return keywordStream
            .map(keyword -> ecc.hashToCurve(keyword.array()))
            .map(hash -> ecc.mul(hash, alpha))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}