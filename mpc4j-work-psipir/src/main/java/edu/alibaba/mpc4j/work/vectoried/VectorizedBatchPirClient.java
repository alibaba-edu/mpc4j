package edu.alibaba.mpc4j.work.vectoried;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirClient;
import edu.alibaba.mpc4j.work.AbstractBatchPirClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * vectorized batch index PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/1/2
 */
public class VectorizedBatchPirClient extends AbstractBatchPirClient {

    /**
     * Vectorized Batch PIR client
     */
    private final VectorizedStdIdxPirClient client;

    public VectorizedBatchPirClient(Rpc clientRpc, Party serverParty, VectorizedBatchPirConfig config) {
        super(VectorizedBatchPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        client = new VectorizedStdIdxPirClient(clientRpc, serverParty, config.getVectorizedBatchPirConfig());
        addSubPto(client);
    }

    @Override
    public void init(int serverElementSize, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(serverElementSize, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        client.init(serverElementSize, 1, maxRetrievalSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<Integer, Boolean> pir(List<Integer> retrievalIndexList) throws MpcAbortException {
        setPtoInput(retrievalIndexList);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int[] xs = IntStream.range(0, retrievalSize).map(retrievalIndexList::get).toArray();
        byte[][] entries = client.pir(xs);
        Map<Integer, Boolean> result = IntStream.range(0, retrievalSize)
            .filter(i -> entries[i] != null)
            .boxed()
            .collect(
                Collectors.toMap(
                    retrievalIndexList::get, i -> BinaryUtils.getBoolean(entries[i], 7), (a, b) -> b,
                    () -> new HashMap<>(retrievalSize))
            );
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime, "Client executes Vectorized Batch PIR");

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }
}
