package edu.alibaba.work.femur.naive;

import com.carrotsearch.hppc.LongArrayList;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex.LongApproxPgmIndexBuilder;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.work.femur.AbstractFemurRpcPirServer;
import gnu.trove.map.TLongObjectMap;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.work.femur.naive.NaiveFemurRpcPirPtoDesc.PtoStep;
import static edu.alibaba.work.femur.naive.NaiveFemurRpcPirPtoDesc.getInstance;

/**
 * PGM-index range naive PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class NaiveFemurRpcPirServer extends AbstractFemurRpcPirServer {
    /**
     * key-value array
     */
    private byte[][] keyValueArray;
    /**
     * response time
     */
    private long genResponseTime;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;

    public NaiveFemurRpcPirServer(Rpc serverRpc, Party clientParty, NaiveFemurRpcPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        pgmIndexLeafEpsilon = config.getPgmIndexLeafEpsilon();
    }

    @Override
    public void init(TLongObjectMap<byte[]> keyValueMap, int l, int maxBatchNum) {
        setInitInput(keyValueMap, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        long[] keyArray = keyValueMap.keys();
        Arrays.sort(keyArray);
        LongArrayList keyList = new LongArrayList();
        keyList.add(keyArray, 0, n);
        // create PGM-index
        LongApproxPgmIndexBuilder builder = new LongApproxPgmIndexBuilder()
            .setSortedKeys(keyList)
            .setEpsilon(pgmIndexLeafEpsilon)
            .setEpsilonRecursive(CommonConstants.PGM_INDEX_RECURSIVE_EPSILON);
        LongApproxPgmIndex pgmIndex = builder.build();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_PGM_INFO.ordinal(), Collections.singletonList(pgmIndex.toByteArray()));
        keyValueArray = new byte[n][Long.BYTES + byteL];
        IntStream.range(0, n).forEach(i -> {
            long key = keyArray[i];
            byte[] keyBytes = LongUtils.longToByteArray(key);
            System.arraycopy(keyBytes, 0, keyValueArray[i], 0, Long.BYTES);
            System.arraycopy(keyValueMap.get(key), 0, keyValueArray[i], Long.BYTES, byteL);
        });
        genResponseTime = 0;
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            answer();
        }
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void answer() throws MpcAbortException {
        List<byte[]> queryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(queryPayload.size() == 2);
        StopWatch genResponseStopWatch = new StopWatch();
        genResponseStopWatch.start();
        int leftRange = IntUtils.byteArrayToInt(queryPayload.get(0));
        int rangeBound = IntUtils.byteArrayToInt(queryPayload.get(1));
        List<byte[]> responsePayload = new ArrayList<>();
        for (int i = 0; i < rangeBound; i++) {
            byte[] response = new byte[Long.BYTES + byteL];
            int idx = (leftRange + i) % n;
            if (idx < 0) {
                idx = idx + n;
            }
            System.arraycopy(keyValueArray[idx], 0, response, 0, Long.BYTES + byteL);
            responsePayload.add(response);
        }
        genResponseStopWatch.stop();
        genResponseTime += genResponseStopWatch.getTime(TimeUnit.MILLISECONDS);
        genResponseStopWatch.reset();
        sendOtherPartyEqualSizePayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }

    public long getGenResponseTime() {
        return genResponseTime;
    }
}