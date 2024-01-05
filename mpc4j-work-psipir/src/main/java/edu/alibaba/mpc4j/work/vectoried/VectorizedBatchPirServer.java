package edu.alibaba.mpc4j.work.vectoried;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirServer;
import edu.alibaba.mpc4j.work.AbstractBatchPirServer;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * vectorized batch index PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/1/2
 */
public class VectorizedBatchPirServer extends AbstractBatchPirServer {

    /**
     * Vectorized Batch PIR server
     */
    private final Mr23BatchIndexPirServer server;

    public VectorizedBatchPirServer(Rpc serverRpc, Party clientParty, VectorizedBatchPirConfig config) {
        super(VectorizedBatchPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        server = new Mr23BatchIndexPirServer(serverRpc, clientParty, config.getVectorizedBatchPirConfig());
    }

    @Override
    public void init(BitVector database, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(database, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        BigInteger[] elementArray = IntStream.range(0, num)
            .mapToObj(i -> database.get(i) ? BigInteger.ONE : BigInteger.ZERO)
            .toArray(BigInteger[]::new);
        NaiveDatabase database1 = NaiveDatabase.create(1, elementArray);
        server.init(database1, maxRetrievalSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        server.pir();
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server executes Vectorized Batch PIR");

        logPhaseInfo(PtoState.PTO_END);
    }
}