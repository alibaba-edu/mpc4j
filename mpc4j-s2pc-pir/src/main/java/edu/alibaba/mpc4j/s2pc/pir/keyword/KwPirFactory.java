package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22.Aaag22KwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22.Aaag22KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22.Aaag22KwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirServer;

/**
 * Keyword PIR factory.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class KwPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private KwPirFactory() {
        // empty
    }

    /**
     * keyword PIR type
     */
    public enum KwPirType {
        /**
         * CMG21
         */
        CMG21,
        /**
         * AAAG22
         */
        AAAG22,
        /**
         * ALPR21
         */
        ALPR21,
    }

    /**
     * create keyword PIR server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return keyword PIR server.
     */
    public static KwPirServer createServer(Rpc serverRpc, Party clientParty, KwPirConfig config) {
        KwPirType type = config.getProType();
        switch (type) {
            case CMG21:
                return new Cmg21KwPirServer(serverRpc, clientParty, (Cmg21KwPirConfig) config);
            case AAAG22:
                return new Aaag22KwPirServer(serverRpc, clientParty, (Aaag22KwPirConfig) config);
            case ALPR21:
                return new Alpr21KwPirServer(serverRpc, clientParty, (Alpr21KwPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + KwPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create keyword PIR client.
     *
     * @param clientRpc   client rpc.
     * @param serverParty server party.
     * @param config      config.
     * @return keyword PIR client.
     */
    public static KwPirClient createClient(Rpc clientRpc, Party serverParty, KwPirConfig config) {
        KwPirType type = config.getProType();
        switch (type) {
            case CMG21:
                return new Cmg21KwPirClient(clientRpc, serverParty, (Cmg21KwPirConfig) config);
            case AAAG22:
                return new Aaag22KwPirClient(clientRpc, serverParty, (Aaag22KwPirConfig) config);
            case ALPR21:
                return new Alpr21KwPirClient(clientRpc, serverParty, (Alpr21KwPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + KwPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create default config.
     *
     * @param securityModel security model.
     * @return default config.
     */
    public static KwPirConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Aaag22KwPirConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Cmg21KwPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
