package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai.PaiSingleCpCksPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai.PaiSingleCpCksPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai.PaiSingleCpCksPirServer;

/**
 * Single client-specific preprocessing KSPIR factory.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class SingleCpKsPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SingleCpKsPirFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum SingleCpKsPirType {
        /**
         * PAI (CKS)
         */
        PAI_CKS,
        /**
         * ALPR21
         */
        ALPR21,
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static <T> SingleCpKsPirServer<T> createServer(Rpc serverRpc, Party clientParty, SingleCpKsPirConfig config) {
        SingleCpKsPirType type = config.getPtoType();
        switch (type) {
            case PAI_CKS:
                return new PaiSingleCpCksPirServer<>(serverRpc, clientParty, (PaiSingleCpCksPirConfig) config);
            case ALPR21:
                return new Alpr21SingleCpKsPirServer<>(serverRpc, clientParty, (Alpr21SingleCpKsPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleCpKsPirType.class.getSimpleName() + ": " + type.name()
                );
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
    public static <T> SingleCpKsPirClient<T> createClient(Rpc clientRpc, Party serverParty, SingleCpKsPirConfig config) {
        SingleCpKsPirType type = config.getPtoType();
        switch (type) {
            case PAI_CKS:
                return new PaiSingleCpCksPirClient<>(clientRpc, serverParty, (PaiSingleCpCksPirConfig) config);
            case ALPR21:
                return new Alpr21SingleCpKsPirClient<>(clientRpc, serverParty, (Alpr21SingleCpKsPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleCpKsPirType.class.getSimpleName() + ": " + type.name()
                );
        }
    }
}
