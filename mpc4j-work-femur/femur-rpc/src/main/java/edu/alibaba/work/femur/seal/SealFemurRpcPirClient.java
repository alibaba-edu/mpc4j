package edu.alibaba.work.femur.seal;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.sampler.integral.geometric.ApacheGeometricSampler;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.work.femur.AbstractFemurRpcPirClient;
import edu.alibaba.work.femur.FemurSealPirNativeUtils;
import edu.alibaba.work.femur.FemurSealPirParams;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.work.femur.seal.SealFemurRpcPirPtoDesc.PtoStep;
import static edu.alibaba.work.femur.seal.SealFemurRpcPirPtoDesc.getInstance;

/**
 * PGM-index range SEAL PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public class SealFemurRpcPirClient extends AbstractFemurRpcPirClient {

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
     * PGM-index
     */
    private LongApproxPgmIndex pgmIndex;
    /**
     * max plaintext size
     */
    private int maxPlaintextSize;
    /**
     * whether to use differential privacy
     */
    private final boolean dp;
    /**
     * query time
     */
    private long genQueryTime;
    /**
     * handle response time
     */
    private long handleResponseTime;
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

    public SealFemurRpcPirClient(Rpc clientRpc, Party serverParty, SealFemurRpcPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        params = config.getParams();
        dp = config.isDp();
        pgmIndexLeafEpsilon = config.getPgmIndexLeafEpsilon();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // generate key pair
        keyGen();
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
        // receive PGM-index info
        List<byte[]> pgmIndexPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_PGM_INFO.ordinal());
        MpcAbortPreconditions.checkArgument(pgmIndexPayload.size() == 1);
        pgmIndex = LongApproxPgmIndex.fromByteArray(pgmIndexPayload.get(0));
        genQueryTime = 0;
        handleResponseTime = 0;
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(long[] keys, int rangeBound, double epsilon) throws MpcAbortException {
        setPtoInput(keys, rangeBound, epsilon);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // PGM range <= range bound
        assert (LongApproxPgmIndex.bound(pgmIndexLeafEpsilon) <= rangeBound);
        maxPlaintextSize = CommonUtils.getUnitNum(n, elementSizeOfPlaintext);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(keys[i]);
        }
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = recover(keys[i]);
        }
        stopWatch.stop();
        long handleResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, handleResponseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    @Override
    public long getGenQueryTime() {
        return genQueryTime;
    }

    @Override
    public long getHandleResponseTime() {
        return handleResponseTime;
    }

    private void keyGen() {
        List<byte[]> keyPair = FemurSealPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 3);
        publicKey = keyPair.get(0);
        secretKey = keyPair.get(1);
        // client sends Galois keys
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), Collections.singletonList(keyPair.get(2)));
    }

    private byte[] recover(long key) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        StopWatch handleResponseStopWatch = new StopWatch();
        handleResponseStopWatch.start();
        int responseSize = IntStream.range(0, params.getDimension() - 1)
            .map(i -> params.getExpansionRatio())
            .reduce(1, (a, b) -> a * b);
        responseSize *= partitionNum;
        MpcAbortPreconditions.checkArgument(responsePayload.size() == responseSize);
        int[] index = pgmIndex.approximateIndexRangeOf(key);
        byte[] entry = null;
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
                        byte[] temp = new byte[Long.BYTES];
                        System.arraycopy(paddingBytes, i * partitionByteL, temp, 0, Long.BYTES);
                        keyEntries[i] = LongUtils.byteArrayToLong(temp);
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
                    entry = valueEntries[i];
                    break;
                }
            }
        }
        handleResponseStopWatch.stop();
        handleResponseTime += handleResponseStopWatch.getTime(TimeUnit.MILLISECONDS);
        handleResponseStopWatch.reset();
        return entry;
    }

    private void query(long key) {
        StopWatch queryStopWatch = new StopWatch();
        queryStopWatch.start();
        int[] index = pgmIndex.approximateIndexRangeOf(key);
        int leftRange, plaintextSize, range = rangeBound;
        int[] indices, dimensionSize;
        double b = 2.0 * rangeBound / epsilon;
        ApacheGeometricSampler sampler = new ApacheGeometricSampler(0, b);
        if (index[0] >= 0) {
            int leftBound = secureRandom.nextInt(
                LongApproxPgmIndex.leftBound(pgmIndexLeafEpsilon),
                rangeBound - LongApproxPgmIndex.rightBound(pgmIndexLeafEpsilon)
            );
            leftRange = index[0] - leftBound;
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
        List<byte[]> queryPayload = new ArrayList<>(FemurSealPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, indices, dimensionSize
        ));
        queryPayload.add(IntUtils.intToByteArray(leftRange));
        queryPayload.add(IntUtils.intToByteArray(plaintextSize));
        queryStopWatch.stop();
        genQueryTime += queryStopWatch.getTime(TimeUnit.MILLISECONDS);
        queryStopWatch.reset();
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }
}