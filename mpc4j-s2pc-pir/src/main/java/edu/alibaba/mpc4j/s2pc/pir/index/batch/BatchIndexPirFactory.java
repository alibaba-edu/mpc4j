package edu.alibaba.mpc4j.s2pc.pir.index.batch;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.naive.NaiveBatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.naive.NaiveBatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.naive.NaiveBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir.Lpzl24BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir.Lpzl24BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir.Lpzl24BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirServer;

/**
 * batch index PIR factory.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class BatchIndexPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private BatchIndexPirFactory() {
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
        /**
         * native batch PIR
         */
        NAIVE_BATCH_PIR,
    }

    /**
     * create server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return server.
     */
    public static BatchIndexPirServer createServer(Rpc serverRpc, Party clientParty, BatchIndexPirConfig config) {
        BatchIndexPirType type = config.getPtoType();
        switch (type) {
            case PSI_PIR:
                return new Lpzl24BatchIndexPirServer(serverRpc, clientParty, (Lpzl24BatchIndexPirConfig) config);
            case VECTORIZED_BATCH_PIR:
                return new Mr23BatchIndexPirServer(serverRpc, clientParty, (Mr23BatchIndexPirConfig) config);
            case NAIVE_BATCH_PIR:
                return new NaiveBatchIndexPirServer(serverRpc, clientParty, (NaiveBatchIndexPirConfig) config);
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
    public static BatchIndexPirClient createClient(Rpc clientRpc, Party serverParty, BatchIndexPirConfig config) {
        BatchIndexPirType type = config.getPtoType();
        switch (type) {
            case PSI_PIR:
                return new Lpzl24BatchIndexPirClient(clientRpc, serverParty, (Lpzl24BatchIndexPirConfig) config);
            case VECTORIZED_BATCH_PIR:
                return new Mr23BatchIndexPirClient(clientRpc, serverParty, (Mr23BatchIndexPirConfig) config);
            case NAIVE_BATCH_PIR:
                return new NaiveBatchIndexPirClient(clientRpc, serverParty, (NaiveBatchIndexPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BatchIndexPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
