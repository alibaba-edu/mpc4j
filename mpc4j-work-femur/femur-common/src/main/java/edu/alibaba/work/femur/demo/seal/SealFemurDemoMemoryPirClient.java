package edu.alibaba.work.femur.demo.seal;

import edu.alibaba.mpc4j.common.sampler.integral.geometric.ApacheGeometricSampler;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.work.femur.FemurSealPirNativeUtils;
import edu.alibaba.work.femur.FemurSealPirParams;
import edu.alibaba.work.femur.demo.AbstractFemurDemoPirClient;
import edu.alibaba.work.femur.demo.FemurStatus;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * SEAL Femur demo PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class SealFemurDemoMemoryPirClient extends AbstractFemurDemoPirClient {

    static {
        System.loadLibrary("femur-native-fhe");
    }

    /**
     * params
     */
    private final FemurSealPirParams params;
    /**
     * element size per BFV plaintext
     */
    private int elementSizeOfPlaintext;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * private key
     */
    private byte[] secretKey;
    /**
     * whether to use differential privacy
     */
    private final boolean dp;
    /**
     * queried key
     */
    private long key;
    /**
     * response size
     */
    private int responseSize;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;
    /**
     * partition num
     */
    private int partitionNum;
    /**
     * partition byte l
     */
    private int partitionByteL;

    public SealFemurDemoMemoryPirClient(SealFemurDemoMemoryPirConfig config) {
        super(config);
        params = config.getParams();
        dp = config.isDp();
        key = BOT_KEY;
        responseSize = -1;
        pgmIndexLeafEpsilon = config.getPgmIndexLeafEpsilon();
    }

    @Override
    public void setDatabaseParams(List<byte[]> paramsPayload) {
        super.setDatabaseParams(paramsPayload);
        // number of coefficients that can store one element
        partitionNum = 0;
        int partitionL;
        do {
            partitionNum++;
            partitionL = CommonUtils.getUnitNum(l + Long.SIZE, partitionNum);
            partitionByteL = CommonUtils.getByteLength(partitionL);
            int coeffSizeOfElement = CommonUtils.getUnitNum(partitionByteL * Byte.SIZE, params.getPlainModulusBitLength());
            elementSizeOfPlaintext = (params.getPolyModulusDegree() / 2) / coeffSizeOfElement;
        } while (elementSizeOfPlaintext < LongApproxPgmIndex.bound(pgmIndexLeafEpsilon));
    }

    @Override
    public List<byte[]> register(String clientId) {
        setRegisterInput(clientId);
        List<byte[]> keyPair = FemurSealPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 3);
        publicKey = keyPair.get(0);
        secretKey = keyPair.get(1);
        List<byte[]> clientResisterInfo = new ArrayList<>();
        clientResisterInfo.add(clientId.getBytes(CommonConstants.DEFAULT_CHARSET));
        clientResisterInfo.add(keyPair.get(2));
        // client sends Galois keys
        // client sends the client ID and Galois keys to server.
        return clientResisterInfo;
    }


    @Override
    public List<byte[]> query(long key, int t, double epsilon) {
        checkQueryInput(key, t, epsilon, pgmIndexLeafEpsilon);

        // PGM range <= range bound
        assert (LongApproxPgmIndex.bound(pgmIndexLeafEpsilon) <= t);
        int maxPlaintextSize = CommonUtils.getUnitNum(n, elementSizeOfPlaintext);
        double b = 2.0 * t / epsilon;
        ApacheGeometricSampler sampler = new ApacheGeometricSampler(0, b);
        this.key = key;
        int[] index = pgmIndexPair.getValue().approximateIndexRangeOf(key);
        int leftRange, plaintextSize, range = t;
        int[] indices, dimensionSize;
        if (index[0] >= 0) {
            int leftBound = secureRandom.nextInt(
                LongApproxPgmIndex.leftBound(pgmIndexLeafEpsilon),
                t - LongApproxPgmIndex.rightBound(pgmIndexLeafEpsilon)
            );
            leftRange = index[0] - secureRandom.nextInt(
                LongApproxPgmIndex.leftBound(pgmIndexLeafEpsilon),
                t - LongApproxPgmIndex.rightBound(pgmIndexLeafEpsilon)
            );
            if (dp) {
                int leftNoise = Math.abs(sampler.sample());
                if (leftNoise + leftBound > n) {
                    leftNoise = n - leftBound - 1;
                }
                int rightNoise = Math.abs(sampler.sample());
                if (rightNoise > n) {
                    rightNoise = n - 1;
                }
                leftRange = leftRange - leftNoise;
                if (leftNoise + rightNoise + range > n) {
                    range = n;
                } else {
                    range = range + leftNoise + rightNoise;
                }
            }
            if (leftRange < 0) {
                leftRange = (leftRange - elementSizeOfPlaintext) / elementSizeOfPlaintext;
            } else {
                leftRange = leftRange / elementSizeOfPlaintext;
            }
            plaintextSize = CommonUtils.getUnitNum(range + elementSizeOfPlaintext, elementSizeOfPlaintext);
            if (plaintextSize > maxPlaintextSize) {
                plaintextSize = maxPlaintextSize;
            }
            dimensionSize = FemurSealPirNativeUtils.computeDimensionLength(plaintextSize, params.getDimension());
            // convert entry index to plaintext index
            int leftIdx = (int) ((((long) index[1] - ((long) leftRange * elementSizeOfPlaintext)) / elementSizeOfPlaintext) % plaintextSize);
            if (leftIdx < 0) {
                leftIdx = leftIdx + plaintextSize;
            }
            // compute indices for each dimension
            indices = FemurSealPirNativeUtils.decomposeIndex(leftIdx, dimensionSize);
        } else {
            if (dp) {
                long leftNoise = Math.abs(sampler.sample());
                long rightNoise = Math.abs(sampler.sample());
                if (leftNoise + rightNoise + range > Integer.MAX_VALUE) {
                    range = n;
                } else {
                    range = range + (int) leftNoise + (int) rightNoise;
                }
            }
            plaintextSize = CommonUtils.getUnitNum(range + elementSizeOfPlaintext, elementSizeOfPlaintext);
            if (plaintextSize > maxPlaintextSize) {
                plaintextSize = maxPlaintextSize;
            }
            dimensionSize = FemurSealPirNativeUtils.computeDimensionLength(plaintextSize, params.getDimension());
            int dummyIdx = secureRandom.nextInt(elementSizeOfPlaintext);
            indices = FemurSealPirNativeUtils.decomposeIndex(dummyIdx, dimensionSize);
            leftRange = secureRandom.nextInt(elementSizeOfPlaintext);
        }
        responseSize = IntStream.range(0, params.getDimension() - 1)
            .map(i -> params.getExpansionRatio())
            .reduce(1, (d1, d2) -> d1 * d2);
        responseSize *= partitionNum;
        // (ClientID, version, query, leftBound, plaintextSize)
        List<byte[]> queryPayload = new ArrayList<>();
        queryPayload.add(clientId.getBytes(CommonConstants.DEFAULT_CHARSET));
        queryPayload.add(pgmIndexPair.getKey().getBytes(CommonConstants.DEFAULT_CHARSET));
        queryPayload.addAll(FemurSealPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, indices, dimensionSize
        ));
        queryPayload.add(IntUtils.intToByteArray(leftRange));
        queryPayload.add(IntUtils.intToByteArray(plaintextSize));

        return queryPayload;
    }

    @Override
    public Pair<FemurStatus, byte[]> retrieve(Pair<FemurStatus, List<byte[]>> response) {
        FemurStatus femurStatus = response.getKey();
        switch (femurStatus) {
            case HINT_V_MISMATCH, CLIENT_NOT_REGS -> {
                return Pair.of(femurStatus, null);
            }
            case SERVER_SUCC_RES -> {
                assert key != BOT_KEY;
                assert responseSize >= 0;
                List<byte[]> responsePayload = response.getValue();
                int[] index = pgmIndexPair.getValue().approximateIndexRangeOf(key);
                if (index[0] >= 0) {
                    assert responsePayload.size() == responseSize;
                    int length = responseSize / partitionNum;
                    long[] keyEntries = new long[elementSizeOfPlaintext * 2];
                    byte[][] valueEntries = new byte[elementSizeOfPlaintext * 2][byteL];
                    for (int j = 0; j < partitionNum; j++) {
                        long[] coeffs = FemurSealPirNativeUtils.decryptReply(
                            params.getEncryptionParams(),
                            secretKey,
                            responsePayload.subList(length * j, length * (j + 1)),
                            params.getDimension()
                        );
                        int byteSizeOfPlaintext = elementSizeOfPlaintext * partitionByteL;
                        byte[] paddingBytes = new byte[byteSizeOfPlaintext * 2];
                        byte[] bytes = FemurSealPirNativeUtils.convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
                        System.arraycopy(bytes, 0, paddingBytes, 0, Math.min(bytes.length, byteSizeOfPlaintext * 2));
                        if (j == 0) {
                            for (int i = 0; i < elementSizeOfPlaintext * 2; i++) {
                                byte[] key = new byte[Long.BYTES];
                                System.arraycopy(paddingBytes, i * partitionByteL, key, 0, Long.BYTES);
                                keyEntries[i] = LongUtils.byteArrayToLong(key);
                            }
                            for (int i = 0; i < elementSizeOfPlaintext * 2; i++) {
                                for (int k = 0; k < partitionByteL - Long.BYTES; k++) {
                                    valueEntries[i][k] = paddingBytes[Long.BYTES + i * partitionByteL + k];
                                }
                            }
                        } else {
                            for (int i = 0; i < elementSizeOfPlaintext * 2; i++) {
                                for (int k = 0; k < partitionByteL; k++) {
                                    if (partitionByteL - Long.BYTES + k + (j - 1) * partitionByteL < byteL) {
                                        valueEntries[i][partitionByteL - Long.BYTES + k + (j - 1) * partitionByteL] = paddingBytes[i * partitionByteL + k];
                                    }
                                }
                            }
                        }
                    }
                    for (int i = 0; i < elementSizeOfPlaintext * 2; i++) {
                        if (keyEntries[i] == key) {
                            key = BOT_KEY;
                            responseSize = -1;
                            return Pair.of(femurStatus, valueEntries[i]);
                        }
                    }
                }
                key = BOT_KEY;
                responseSize = -1;
                return Pair.of(FemurStatus.SERVER_SUCC_RES, null);
            }
            default -> throw new IllegalStateException("Invalid state " + femurStatus);
        }
    }
}