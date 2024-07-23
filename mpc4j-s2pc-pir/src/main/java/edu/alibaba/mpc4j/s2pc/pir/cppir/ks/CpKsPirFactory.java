package edu.alibaba.mpc4j.s2pc.pir.cppir.ks;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai.PaiCpCksPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai.PaiCpCksPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai.PaiCpCksPirServer;

/**
 * client-specific preprocessing KSPIR factory.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class CpKsPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private CpKsPirFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum CpKsPirType {
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
    public static <T> CpKsPirServer<T> createServer(Rpc serverRpc, Party clientParty, CpKsPirConfig config) {
        CpKsPirType type = config.getPtoType();
        switch (type) {
            case PAI_CKS -> {
                return new PaiCpCksPirServer<>(serverRpc, clientParty, (PaiCpCksPirConfig) config);
            }
            case ALPR21 -> {
                return new Alpr21CpKsPirServer<>(serverRpc, clientParty, (Alpr21CpKsPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + CpKsPirType.class.getSimpleName() + ": " + type.name());
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
    public static <T> CpKsPirClient<T> createClient(Rpc clientRpc, Party serverParty, CpKsPirConfig config) {
        CpKsPirType type = config.getPtoType();
        switch (type) {
            case PAI_CKS -> {
                return new PaiCpCksPirClient<>(clientRpc, serverParty, (PaiCpCksPirConfig) config);
            }
            case ALPR21 -> {
                return new Alpr21CpKsPirClient<>(clientRpc, serverParty, (Alpr21CpKsPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + CpKsPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
