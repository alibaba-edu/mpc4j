package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirServer;

/**
 * standard KSPIR factory.
 *
 * @author Liqiang Peng
 * @date 2024/7/19
 */
public class StdKsPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private StdKsPirFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum StdKsPirType {
        /**
         * Label_PSI
         */
        Label_PSI,
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static <T> StdKsPirServer<T> createServer(Rpc serverRpc, Party clientParty, StdKsPirConfig config) {
        StdKsPirType type = config.getPtoType();
        switch (type) {
            case Label_PSI -> {
                return new LabelpsiStdKsPirServer<>(serverRpc, clientParty, (LabelpsiStdKsPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + StdKsPirType.class.getSimpleName() + ": " + type.name());
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
    public static <T> StdKsPirClient<T> createClient(Rpc clientRpc, Party serverParty, StdKsPirConfig config) {
        StdKsPirType type = config.getPtoType();
        switch (type) {
            case Label_PSI -> {
                return new LabelpsiStdKsPirClient<>(clientRpc, serverParty, (LabelpsiStdKsPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + StdKsPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
