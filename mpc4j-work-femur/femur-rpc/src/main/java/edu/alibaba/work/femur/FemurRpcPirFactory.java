package edu.alibaba.work.femur;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.work.femur.naive.NaiveFemurRpcPirClient;
import edu.alibaba.work.femur.naive.NaiveFemurRpcPirConfig;
import edu.alibaba.work.femur.naive.NaiveFemurRpcPirServer;
import edu.alibaba.work.femur.seal.SealFemurRpcPirClient;
import edu.alibaba.work.femur.seal.SealFemurRpcPirConfig;
import edu.alibaba.work.femur.seal.SealFemurRpcPirServer;

/**
 * PGM-index range keyword PIR factory.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public class FemurRpcPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private FemurRpcPirFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum FemurPirType {
        /**
         * PGM-index SEAL PIR
         */
        PGM_INDEX_SEAL_PIR,
        /**
         * PGM-index naive PIR
         */
        PGM_INDEX_NAIVE_PIR,
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static FemurRpcPirServer createServer(Rpc serverRpc, Party clientParty, FemurRpcPirConfig config) {
        FemurPirType type = config.getPtoType();
        switch (type) {
            case PGM_INDEX_SEAL_PIR -> {
                return new SealFemurRpcPirServer(serverRpc, clientParty, (SealFemurRpcPirConfig) config);
            }
            case PGM_INDEX_NAIVE_PIR -> {
                return new NaiveFemurRpcPirServer(serverRpc, clientParty, (NaiveFemurRpcPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + FemurPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create a client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static FemurRpcPirClient createClient(Rpc clientRpc, Party serverParty, FemurRpcPirConfig config) {
        FemurPirType type = config.getPtoType();
        switch (type) {
            case PGM_INDEX_SEAL_PIR -> {
                return new SealFemurRpcPirClient(clientRpc, serverParty, (SealFemurRpcPirConfig) config);
            }
            case PGM_INDEX_NAIVE_PIR -> {
                return new NaiveFemurRpcPirClient(clientRpc, serverParty, (NaiveFemurRpcPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + FemurPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
