package edu.alibaba.mpc4j.work;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.work.psipir.Lpzl24BatchPirClient;
import edu.alibaba.mpc4j.work.psipir.Lpzl24BatchPirConfig;
import edu.alibaba.mpc4j.work.psipir.Lpzl24BatchPirServer;
import edu.alibaba.mpc4j.work.vectoried.VectorizedBatchPirClient;
import edu.alibaba.mpc4j.work.vectoried.VectorizedBatchPirConfig;
import edu.alibaba.mpc4j.work.vectoried.VectorizedBatchPirServer;

/**
 * batch PIR factory.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class BatchPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private BatchPirFactory() {
        // empty
    }

    /**
     * batch index PIR type
     */
    public enum BatchIndexPirType {
        /**
         * PSI_PIR
         */
        PSI_PIR,
        /**
         * Vectorized Batch PIR
         */
        VECTORIZED_BATCH_PIR,
    }

    /**
     * create server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return server.
     */
    public static BatchPirServer createServer(Rpc serverRpc, Party clientParty, BatchPirConfig config) {
        BatchIndexPirType type = config.getPtoType();
        switch (type) {
            case PSI_PIR:
                return new Lpzl24BatchPirServer(serverRpc, clientParty, (Lpzl24BatchPirConfig) config);
            case VECTORIZED_BATCH_PIR:
                return new VectorizedBatchPirServer(serverRpc, clientParty, (VectorizedBatchPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BatchIndexPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create client.
     *
     * @param clientRpc   client rpc.
     * @param serverParty server party.
     * @param config      config.
     * @return client.
     */
    public static BatchPirClient createClient(Rpc clientRpc, Party serverParty, BatchPirConfig config) {
        BatchIndexPirType type = config.getPtoType();
        switch (type) {
            case PSI_PIR:
                return new Lpzl24BatchPirClient(clientRpc, serverParty, (Lpzl24BatchPirConfig) config);
            case VECTORIZED_BATCH_PIR:
                return new VectorizedBatchPirClient(clientRpc, serverParty, (VectorizedBatchPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BatchIndexPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
