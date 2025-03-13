package edu.alibaba.work.femur.naive;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.sampler.integral.geometric.ApacheGeometricSampler;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.work.femur.AbstractFemurRpcPirClient;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.work.femur.naive.NaiveFemurRpcPirPtoDesc.PtoStep;
import static edu.alibaba.work.femur.naive.NaiveFemurRpcPirPtoDesc.getInstance;

/**
 * PGM-index range naive PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class NaiveFemurRpcPirClient extends AbstractFemurRpcPirClient {
    /**
     * PGM-index
     */
    private LongApproxPgmIndex pgmIndex;
    /**
     * whether to use differential privacy
     */
    private final boolean dp;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;
    /**
     * response size
     */
    private int[] responseSize;
    /**
     * query time
     */
    private long genQueryTime;
    /**
     * handle response time
     */
    private long handleResponseTime;

    public NaiveFemurRpcPirClient(Rpc clientRpc, Party serverParty, NaiveFemurRpcPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        dp = config.getDp();
        pgmIndexLeafEpsilon = config.getPgmIndexLeafEpsilon();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
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
        assert LongApproxPgmIndex.bound(pgmIndexLeafEpsilon) <= rangeBound;
        logPhaseInfo(PtoState.PTO_BEGIN);

        responseSize = new int[batchNum];
        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(keys[i], i);
        }
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = recover(keys[i], responseSize[i]);
        }
        stopWatch.stop();
        long handleResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, handleResponseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private byte[] recover(long key, int responseSize) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyEqualSizePayload(
            PtoStep.SERVER_SEND_RESPONSE.ordinal(), responseSize, Long.BYTES + byteL
        );
        StopWatch handleResponseStopWatch = new StopWatch();
        handleResponseStopWatch.start();
        int[] index = pgmIndex.approximateIndexRangeOf(key);
        byte[] entry = null;
        if (index[0] >= 0) {
            byte[] keyBytes = LongUtils.longToByteArray(key);
            MpcAbortPreconditions.checkArgument(responsePayload.size() == responseSize);
            for (int i = 0; i < responseSize; i++) {
                byte[] keyEntry = responsePayload.get(i);
                assert keyEntry.length == Long.BYTES + byteL;
                if (Arrays.equals(keyBytes, 0, Long.BYTES, keyEntry, 0, Long.BYTES)) {
                    entry = new byte[byteL];
                    System.arraycopy(keyEntry, Long.BYTES, entry, 0, byteL);
                }
            }
        }
        handleResponseStopWatch.stop();
        handleResponseTime += handleResponseStopWatch.getTime(TimeUnit.MILLISECONDS);
        handleResponseStopWatch.reset();
        return entry;
    }

    private void query(long key, int batchIdx) {
        StopWatch queryStopWatch = new StopWatch();
        queryStopWatch.start();
        int[] index = pgmIndex.approximateIndexRangeOf(key);
        int leftRange, range = rangeBound;
        if (index[0] >= 0) {
            leftRange = index[0] - secureRandom.nextInt(
                LongApproxPgmIndex.leftBound(pgmIndexLeafEpsilon),
                rangeBound - LongApproxPgmIndex.rightBound(pgmIndexLeafEpsilon));
        } else {
            leftRange = secureRandom.nextInt(n);
        }
        List<byte[]> queryPayload = new ArrayList<>();
        if (dp) {
            ApacheGeometricSampler sampler = new ApacheGeometricSampler(0, 2.0 * rangeBound / epsilon);
            int leftNoise = Math.abs(sampler.sample());
            if (leftNoise >= n) {
                leftNoise = n - 1;
            }
            int rightNoise = Math.abs(sampler.sample());
            if (rightNoise >= n) {
                rightNoise = n - 1;
            }
            leftRange = leftRange - leftNoise;
            if (leftNoise + rightNoise + range > n) {
                range = n;
            } else {
                range = range + leftNoise + rightNoise;
            }
        }
        queryPayload.add(IntUtils.intToByteArray(leftRange));
        queryPayload.add(IntUtils.intToByteArray(range));
        responseSize[batchIdx] = range;
        queryStopWatch.stop();
        genQueryTime += queryStopWatch.getTime(TimeUnit.MILLISECONDS);
        queryStopWatch.reset();
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    @Override
    public long getGenQueryTime() {
        return genQueryTime;
    }

    @Override
    public long getHandleResponseTime() {
        return handleResponseTime;
    }
}