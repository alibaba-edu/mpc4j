package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
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
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.AbstractStdKsPirServer;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirPtoDesc.PtoStep;

/**
 * Label PSI standard KSPIR server.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class LabelpsiStdKsPirServer<T> extends AbstractStdKsPirServer<T> {
    /**
     * stream cipher
     */
    private final StreamCipher streamCipher;
    /**
     * Label PSI PIR params
     */
    private final LabelpsiStdKsPirParams params;
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

    public LabelpsiStdKsPirServer(Rpc serverRpc, Party clientParty, LabelpsiStdKsPirConfig config) {
        super(LabelpsiStdKsPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
        streamCipher = StreamCipherFactory.createInstance(envType);
        ivByteLength = 0;
        params = config.getParams();
    }

    @Override
    public void init(Map<T, byte[]> keyValueMap, int l, int maxBatchNum) throws MpcAbortException {
        assert maxBatchNum <= params.maxRetrievalSize();
        setInitInput(keyValueMap, l, maxBatchNum);

        List<byte[]> serverKeysPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal());
        MpcAbortPreconditions.checkArgument(serverKeysPayload.size() == 2, "Failed to receive BFV public keys payload");
        this.publicKey = serverKeysPayload.removeFirst();
        this.relinKeys = serverKeysPayload.removeFirst();

        stopWatch.start();
        // generate prf
        ArrayList<T> keysList = new ArrayList<>(keyValueMap.keySet());
        List<ByteBuffer> keysPrf = computeKeysPrf(keysList);
        Map<ByteBuffer, byte[]> prfLabelMap = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toMap(keysPrf::get, i -> keyValueMap.get(keysList.get(i)), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, oprfTime, "Server computes PRFs");

        stopWatch.start();
        // generate hash bins
        byte[][] hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
        List<List<HashBinEntry<ByteBuffer>>> hashBins = generateCompleteHashBin(keysPrf, params.getBinNum(), hashKeys);
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), cuckooHashKeyPayload);
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
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> blindPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_BLIND.ordinal());
        List<byte[]> blindPrfPayload = handleBlindPayload(blindPayload);
        sendOtherPartyPayload(PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), blindPrfPayload);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Server executes OPRF");

        stopWatch.start();
        answer();
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

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
                if (partition.isEmpty()) {
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
        Stream<List<HashBinEntry<ByteBuffer>>> partitionStream = parallel ? partitions.stream().parallel() : partitions.stream();
        return partitionStream
            .filter(partition -> !partition.isEmpty())
            .peek(partition -> {
                for (int index = partition.size(); index < params.getMaxPartitionSizePerBin(); index++) {
                    partition.add(HashBinEntry.fromEmptyItem(botByteBuffer));
                }
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<List<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(List<ByteBuffer> itemList, int binNum,
                                                                         byte[][] hashKeys) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, n, hashKeys);
        completeHash.insertItems(itemList);
        IntStream intStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        List<List<HashBinEntry<ByteBuffer>>> hashBinList = intStream
            .mapToObj(binIndex -> sortedHashBinEntries(new ArrayList<>(completeHash.getBin(binIndex))))
            .collect(Collectors.toCollection(ArrayList::new));
        binSize = hashBinList.stream().mapToInt(List::size).max().orElse(0);
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botByteBuffer);
        hashBinList.forEach(bin -> {
            int paddingNum = binSize - bin.size();
            for (int index = 0; index < paddingNum; index++) {
                bin.add(paddingEntry);
            }
        });
        return hashBinList;
    }

    private boolean checkRepeatedItemPart(List<Set<Long>> existingItemParts, long[] itemParts) {
        assert existingItemParts.size() == itemParts.length;
        return IntStream.range(0, itemParts.length).anyMatch(i -> existingItemParts.get(i).contains(itemParts[i]));
    }

    private void encodeDatabase(Map<ByteBuffer, byte[]> prfMap, List<List<HashBinEntry<ByteBuffer>>> hashBins) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, params.getPlainModulus());
        int itemPerCiphertext = params.getItemPerCiphertext();
        int itemEncodedSlotSize = params.getItemEncodedSlotSize();
        int partitionCount = CommonUtils.getUnitNum(binSize, params.getMaxPartitionSizePerBin());
        int bigPartitionCount = binSize / params.getMaxPartitionSizePerBin();
        int labelPartitionCount = CommonUtils.getUnitNum((byteL + ivByteLength) * Byte.SIZE,
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
                IntStream itemIndexStream = parallel ? IntStream.range(0, itemPerCiphertext).parallel() : IntStream.range(0, itemPerCiphertext);
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
                            byte[] ciphertextLabel = new byte[ivByteLength + byteL];
                            System.arraycopy(
                                extendedCipherLabel, CommonConstants.BLOCK_BYTE_LENGTH - ivByteLength, ciphertextLabel, 0, ivByteLength + byteL
                            );
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
                serverKeywordEncode.add(LabelpsiStdKsPirNativeUtils.preprocessDatabase(
                    params.getEncryptionParams(), encodeElementVector, params.getPsLowDegree())
                );
                for (int j = 0; j < labelPartitionCount; j++) {
                    serverLabelEncode.add(LabelpsiStdKsPirNativeUtils.preprocessDatabase(
                        params.getEncryptionParams(), encodeLabelVector[j], params.getPsLowDegree()
                    ));
                }
            }
        }
    }

    private List<byte[]> handleBlindPayload(List<byte[]> blindElements) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(!blindElements.isEmpty());
        Stream<byte[]> blindStream = parallel ? blindElements.stream().parallel() : blindElements.stream();
        return blindStream
            // compute H(m_c)^βα
            .map(element -> ecc.mul(element, alpha))
            .collect(Collectors.toList());
    }

    private void answer() throws MpcAbortException {
        List<byte[]> queryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == params.getCiphertextNum() * params.getQueryPowers().length,
            "The size of query is incorrect"
        );
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
        int labelPartitionCount = CommonUtils.getUnitNum((byteL + ivByteLength) * Byte.SIZE,
            (PirUtils.getBitLength(params.getPlainModulus()) - 1) * params.getItemEncodedSlotSize());
        IntStream queryIntStream = parallel ?
            IntStream.range(0, params.getCiphertextNum()).parallel() : IntStream.range(0, params.getCiphertextNum());
        List<byte[]> queryPowers = queryIntStream
            .mapToObj(i -> LabelpsiStdKsPirNativeUtils.computeEncryptedPowers(
                params.getEncryptionParams(),
                relinKeys,
                queryPayload.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree()))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        List<byte[]> keywordResponsePayload;
        List<byte[]> labelResponsePayload;
        if (params.getPsLowDegree() > 0) {
            keywordResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            LabelpsiStdKsPirNativeUtils.optComputeMatches(
                                params.getEncryptionParams(),
                                publicKey,
                                relinKeys,
                                serverKeywordEncode.get(i * partitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            sendOtherPartyPayload(PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), keywordResponsePayload);
            labelResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            LabelpsiStdKsPirNativeUtils.optComputeMatches(
                                params.getEncryptionParams(),
                                publicKey,
                                relinKeys,
                                serverLabelEncode.get(i * partitionCount * labelPartitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            sendOtherPartyPayload(PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), labelResponsePayload);
        } else if (params.getPsLowDegree() == 0) {
            keywordResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            LabelpsiStdKsPirNativeUtils.naiveComputeMatches(
                                params.getEncryptionParams(),
                                publicKey,
                                serverKeywordEncode.get(i * partitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            sendOtherPartyPayload(PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), keywordResponsePayload);
            labelResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            LabelpsiStdKsPirNativeUtils.naiveComputeMatches(
                                params.getEncryptionParams(),
                                publicKey,
                                serverLabelEncode.get(i * partitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            sendOtherPartyPayload(PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), labelResponsePayload);
        } else {
            throw new MpcAbortException("ps_low_degree is incorrect.");
        }
    }

    private List<ByteBuffer> computeKeysPrf(ArrayList<T> keys) {
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        Stream<T> keysStream = parallel ? keys.stream().parallel() : keys.stream();
        return keysStream
            .map(ObjectUtils::objectToByteArray)
            .map(ecc::hashToCurve)
            .map(hash -> ecc.mul(hash, alpha))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}