package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon.PantheonStdKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon.PantheonStdKwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon.PantheonStdKwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21.Alpr21StdKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21.Alpr21StdKwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21.Alpr21StdKwPirServer;

/**
 * standard keyword PIR factory.
 *
 * @author Liqiang Peng
 * @date 2024/7/19
 */
public class StdKwPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private StdKwPirFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum StdKwPirType {
        /**
         * Pantheon
         */
        Pantheon,
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
    public static <T> StdKwPirServer<T> createServer(Rpc serverRpc, Party clientParty, StdKwPirConfig config) {
        StdKwPirType type = config.getPtoType();
        switch (type) {
            case Pantheon -> {
                return new PantheonStdKwPirServer<>(serverRpc, clientParty, (PantheonStdKwPirConfig) config);
            }
            case ALPR21 -> {
                return new Alpr21StdKwPirServer<>(serverRpc, clientParty, (Alpr21StdKwPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + StdKwPirType.class.getSimpleName() + ": " + type.name());
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
    public static <T> StdKwPirClient<T> createClient(Rpc clientRpc, Party serverParty, StdKwPirConfig config) {
        StdKwPirType type = config.getPtoType();
        switch (type) {
            case Pantheon -> {
                return new PantheonStdKwPirClient<>(clientRpc, serverParty, (PantheonStdKwPirConfig) config);
            }
            case ALPR21 -> {
                return new Alpr21StdKwPirClient<>(clientRpc, serverParty, (Alpr21StdKwPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + StdKwPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
