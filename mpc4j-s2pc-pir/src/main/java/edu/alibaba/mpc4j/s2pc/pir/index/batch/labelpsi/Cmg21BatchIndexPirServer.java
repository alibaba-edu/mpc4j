package edu.alibaba.mpc4j.s2pc.pir.index.batch.labelpsi;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowersDag;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirNativeUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.labelpsi.Cmg21BatchIndexPirPtoDesc.PtoStep;

/**
 * CMG21 batch index PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/2/4
 */
public class Cmg21BatchIndexPirServer extends AbstractBatchIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * CMG21 params
     */
    private Cmg21BatchIndexPirParams params;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * relinearization keys
     */
    private byte[] relinKeys;
    /**
     * server encoded index
     */
    private List<List<byte[]>> serverIndexEncode;
    /**
     * server encoded label
     */
    private List<List<byte[]>> serverLabelEncode;
    /**
     * bin size
     */
    private int maxBinSize;
    /**
     * encryption params
     */
    private byte[] encryptionParams;
    /**
     * keyword response
     */
    private List<byte[]> keywordResponsePayload;
    /**
     * label response
     */
    private List<byte[]> labelResponsePayload;
    /**
     * partition count
     */
    private int partitionCount;
    /**
     * power size
     */
    private int powerSize;

    public Cmg21BatchIndexPirServer(Rpc serverRpc, Party clientParty, Cmg21BatchIndexPirConfig config) {
        super(Cmg21BatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(NaiveDatabase database, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(database, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // CMG21 params
        if (num <= 1 << 12) {
            if (maxRetrievalSize <= 1 << 10) {
                params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_12_CLIENT_LOG_SIZE_10.copy();
            } else {
                params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_12_CLIENT_LOG_SIZE_12.copy();
            }
        } else if (num <= 1 << 16) {
            if (maxRetrievalSize <= 1 << 10) {
                params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_16_CLIENT_LOG_SIZE_10.copy();
            } else {
                params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_16_CLIENT_LOG_SIZE_12.copy();
            }
        } else if (num <= 1 << 20) {
            params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_12.copy();
        } else if (num <= 1 << 22) {
            params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_22_CLIENT_LOG_SIZE_12.copy();
        } else {
            params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_12.copy();
        }
        assert maxRetrievalSize <= params.maxClientElementSize() : "retrieval size is larger than the upper bound.";
        assert database.rows() <= params.getPlainModulus();

        DataPacketHeader clientPublicKeysPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeysPayload = rpc.receive(clientPublicKeysPayloadHeader).getPayload();
        MpcAbortPreconditions.checkArgument(publicKeysPayload.size() == 2, "Failed to receive BFV public keys payload");
        this.publicKey = publicKeysPayload.remove(0);
        this.relinKeys = publicKeysPayload.remove(0);
        this.encryptionParams = Cmg21KwPirNativeUtils.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits()
        );

        stopWatch.start();
        // generate hash bins
        byte[][] hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashNum(), secureRandom);
        IntHashBin intHashBin = generateCompleteHashBin(hashKeys);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime, "Server generates hash bins");

        stopWatch.start();
        // encode database
        encodeDatabase(intHashBin, database);
        intHashBin.clear();
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, encodeTime, "Server encodes label");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive client query
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == params.getCiphertextNum() * params.getQueryPowers().length,
            "The size of query is incorrect"
        );

        // generate response
        stopWatch.start();
        List<byte[]> queryPowers = computeEncryptQueryPowers(queryPayload);
        computeResponse(queryPowers);
        if (partitionCount > 1) {
            DataPacketHeader keywordResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(keywordResponseHeader, keywordResponsePayload));
        }
        DataPacketHeader labelResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(labelResponseHeader, labelResponsePayload));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * generate complete hash bins.
     *
     * @param hashKeys hash keys.
     * @return complete hash bins.
     */
    private IntHashBin generateCompleteHashBin(byte[][] hashKeys) {
        int[] index = IntStream.range(0, num).toArray();
        IntHashBin intHashBin = new SimpleIntHashBin(envType, params.getBinNum(), num, hashKeys);
        intHashBin.insertItems(index);
        maxBinSize = IntStream.range(0, intHashBin.binNum()).map(intHashBin::binSize).max().orElse(0);
        partitionCount = CommonUtils.getUnitNum(maxBinSize, params.getMaxPartitionSizePerBin());
        return intHashBin;
    }

    /**
     * for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
     * and coeffs of g(x), which has the property g(y) = label(y) for each y in bucket.
     * ciphertext num is small, therefore we need to do parallel computation inside the loop.
     *
     * @param intHashBin int hash bin.
     * @param database   database.
     */
    private void encodeDatabase(IntHashBin intHashBin, NaiveDatabase database) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, params.getPlainModulus());
        int bigPartitionCount = maxBinSize / params.getMaxPartitionSizePerBin();
        int labelPartitionCount = CommonUtils.getUnitNum(
            elementByteLength * Byte.SIZE, PirUtils.getBitLength(params.getPlainModulus()) - 1
        );
        serverIndexEncode = new ArrayList<>();
        serverLabelEncode = new ArrayList<>();
        for (int i = 0; i < params.getCiphertextNum(); i++) {
            int finalIndex = i;
            for (int partition = 0; partition < partitionCount; partition++) {
                long[][] fCoeffs = new long[params.getItemPerCiphertext()][];
                long[][][] gCoeffs = new long[labelPartitionCount][params.getItemPerCiphertext()][];
                int partitionSize, partitionStart;
                partitionSize = partition < bigPartitionCount ?
                    params.getMaxPartitionSizePerBin() : maxBinSize % params.getMaxPartitionSizePerBin();
                partitionStart = params.getMaxPartitionSizePerBin() * partition;
                IntStream itemIndexStream = IntStream.range(0, params.getItemPerCiphertext());
                itemIndexStream = parallel ? itemIndexStream.parallel() : itemIndexStream;
                itemIndexStream.forEach(j -> {
                    int nonEmptyBuckets = 0;
                    List<Long> currentBucketElement = new ArrayList<>();
                    long[][] currentBucketLabels = new long[labelPartitionCount][partitionSize];
                    for (int l = 0; l < partitionSize; l++) {
                        int index = finalIndex * params.getItemPerCiphertext() + j;
                        if (partitionStart + l < intHashBin.binSize(index)) {
                            long item = intHashBin.getBin(index)[partitionStart + l];
                            if (!currentBucketElement.contains(item)) {
                                nonEmptyBuckets++;
                                currentBucketElement.add(item);
                            }
                        }
                    }
                    long[] xArray = IntStream.range(0, nonEmptyBuckets).mapToLong(currentBucketElement::get).toArray();
                    fCoeffs[j] = nonEmptyBuckets > 0 ? zp64Poly.rootInterpolate(nonEmptyBuckets, xArray, 0L) : new long[0];
                    for (int l = 0; l < nonEmptyBuckets; l++) {
                        int index = Math.toIntExact(currentBucketElement.get(l));
                        long[] temp = encodeLabel(database.getBytesData(index), labelPartitionCount);
                        for (int k = 0; k < labelPartitionCount; k++) {
                            currentBucketLabels[k][l] = temp[k];
                        }
                    }
                    for (int k = 0; k < labelPartitionCount; k++) {
                        long[] yArray = new long[nonEmptyBuckets];
                        System.arraycopy(currentBucketLabels[k], 0, yArray, 0, nonEmptyBuckets);
                        gCoeffs[k][j] = nonEmptyBuckets > 0 ? zp64Poly.interpolate(nonEmptyBuckets, xArray, yArray) : new long[0];
                    }
                });
                long[][] encodeElementVector = new long[partitionSize + 1][params.getPolyModulusDegree()];
                int labelSize = IntStream.range(1, gCoeffs[0].length).map(j -> gCoeffs[0][j].length).filter(j -> j >= 1).max().orElse(1);
                long[][][] encodeLabelVector = new long[labelPartitionCount][labelSize][params.getPolyModulusDegree()];
                for (int j = 0; j < partitionSize + 1; j++) {
                    // encode the jth coefficients of all polynomials into a vector
                    for (int l = 0; l < params.getItemPerCiphertext(); l++) {
                        if (fCoeffs[l].length == 0) {
                            encodeElementVector[j][l] = Math.abs(secureRandom.nextLong()) % params.getPlainModulus();
                        } else {
                            encodeElementVector[j][l] = (j < fCoeffs[l].length) ? fCoeffs[l][j] : 0;
                        }
                    }
                }
                for (int j = 0; j < labelSize; j++) {
                    for (int k = 0; k < labelPartitionCount; k++) {
                        for (int l = 0; l < params.getItemPerCiphertext(); l++) {
                            if (gCoeffs[k][l].length == 0) {
                                encodeLabelVector[k][j][l] = Math.abs(secureRandom.nextLong()) % params.getPlainModulus();
                            } else {
                                encodeLabelVector[k][j][l] = (j < gCoeffs[k][l].length) ? gCoeffs[k][l][j] : 0;
                            }
                        }
                    }
                }
                serverIndexEncode.add(Cmg21KwPirNativeUtils.preprocessDatabase(encryptionParams, encodeElementVector, params.getPsLowDegree()));
                for (int j = 0; j < labelPartitionCount; j++) {
                    serverLabelEncode.add(Cmg21KwPirNativeUtils.preprocessDatabase(encryptionParams, encodeLabelVector[j], params.getPsLowDegree()));
                }
            }
        }
    }

    /**
     * encode label.
     *
     * @param labelBytes   label.
     * @param partitionNum partition num.
     * @return encoded label.
     */
    public long[] encodeLabel(byte[] labelBytes, int partitionNum) {
        long[] encodedArray = new long[partitionNum];
        int shiftBits = CommonUtils.getUnitNum(labelBytes.length * Byte.SIZE, partitionNum);
        BigInteger bigIntLabel = BigIntegerUtils.byteArrayToNonNegBigInteger(labelBytes);
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(shiftBits).subtract(BigInteger.ONE);
        for (int i = 0; i < partitionNum; i++) {
            encodedArray[i] = bigIntLabel.and(shiftMask).longValueExact();
            bigIntLabel = bigIntLabel.shiftRight(shiftBits);
        }
        IntStream.range(0, partitionNum).forEach(i -> {
            assert encodedArray[i] < params.getPlainModulus();
        });
        return encodedArray;
    }

    /**
     * compute encrypt query powers.
     *
     * @param encryptQuery encrypt query.
     * @return encrypt query powers.
     */
    private List<byte[]> computeEncryptQueryPowers(List<byte[]> encryptQuery) {
        int[][] powerDegree;
        if (params.getPsLowDegree() > 0) {
            TIntSet innerPowersSet = new TIntHashSet(params.getQueryPowers().length);
            TIntSet outerPowersSet = new TIntHashSet(params.getQueryPowers().length);
            for (int i = 0; i < params.getQueryPowers().length; i++) {
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
        powerSize = powerDegree.length;
        IntStream queryIntStream = parallel ?
            IntStream.range(0, params.getCiphertextNum()).parallel() : IntStream.range(0, params.getCiphertextNum());
        return queryIntStream
            .mapToObj(i -> Cmg21KwPirNativeUtils.computeEncryptedPowers(
                encryptionParams,
                relinKeys,
                encryptQuery.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree()))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * generate server response.
     *
     * @param encryptedQueryPowers encrypt query powers.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void computeResponse(List<byte[]> encryptedQueryPowers) throws MpcAbortException {
        int labelPartitionCount = CommonUtils.getUnitNum(
            elementByteLength * Byte.SIZE, PirUtils.getBitLength(params.getPlainModulus()) - 1
        );
        if (params.getPsLowDegree() > 0) {
            if (partitionCount > 1) {
                keywordResponsePayload = IntStream.range(0, params.getCiphertextNum())
                    .mapToObj(i ->
                        (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                            .mapToObj(j ->
                                Cmg21KwPirNativeUtils.optComputeMatches(
                                    encryptionParams,
                                    publicKey,
                                    relinKeys,
                                    serverIndexEncode.get(i * partitionCount + j),
                                    encryptedQueryPowers.subList(i * powerSize, (i + 1) * powerSize),
                                    params.getPsLowDegree()))
                            .toArray(byte[][]::new))
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toCollection(ArrayList::new));
            }
            labelResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.optComputeMatches(
                                encryptionParams,
                                publicKey,
                                relinKeys,
                                serverLabelEncode.get(i * partitionCount * labelPartitionCount + j),
                                encryptedQueryPowers.subList(i * powerSize, (i + 1) * powerSize),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        } else if (params.getPsLowDegree() == 0) {
            if (partitionCount > 1) {
                keywordResponsePayload = IntStream.range(0, params.getCiphertextNum())
                    .mapToObj(i ->
                        (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                            .mapToObj(j ->
                                Cmg21KwPirNativeUtils.naiveComputeMatches(
                                    encryptionParams,
                                    publicKey,
                                    serverIndexEncode.get(i * partitionCount + j),
                                    encryptedQueryPowers.subList(i * powerSize, (i + 1) * powerSize)))
                            .toArray(byte[][]::new))
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toCollection(ArrayList::new));
            }
            labelResponsePayload = IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.naiveComputeMatches(
                                encryptionParams,
                                publicKey,
                                serverLabelEncode.get(i * partitionCount * labelPartitionCount + j),
                                encryptedQueryPowers.subList(i * powerSize, (i + 1) * powerSize)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        } else {
            throw new MpcAbortException("ps_low_degree is incorrect.");
        }
    }
}

