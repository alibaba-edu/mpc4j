package edu.alibaba.mpc4j.s2pc.upso.upsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j.Cmg21jUpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j.Cmg21jUpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j.Cmg21jUpsiServer;

/**
 * UPSI factory.
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public class UpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private UpsiFactory() {
        // empty
    }

    /**
     * UPSI type
     */
    public enum UpsiType {
        /**
         * CMG21
         */
        CMG21,
        /**
         * CMG21J
         */
        CMG21J,
    }

    /**
     * create server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return UPSI server.
     */
    public static <T> UpsiServer<T> createServer(Rpc serverRpc, Party clientParty, UpsiConfig config) {
        UpsiType type = config.getPtoType();
        switch (type) {
            case CMG21:
                return new Cmg21UpsiServer<>(serverRpc, clientParty, (Cmg21UpsiConfig) config);
            case CMG21J:
                return new Cmg21jUpsiServer<>(serverRpc, clientParty, (Cmg21jUpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UpsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create client.
     *
     * @param clientRpc   client rpc.
     * @param serverParty server party.
     * @param config      config.
     * @return UPSI client.
     */
    public static <T> UpsiClient<T> createClient(Rpc clientRpc, Party serverParty, UpsiConfig config) {
        UpsiType type = config.getPtoType();
        switch (type) {
            case CMG21:
                return new Cmg21UpsiClient<>(clientRpc, serverParty, (Cmg21UpsiConfig) config);
            case CMG21J:
                return new Cmg21jUpsiClient<>(clientRpc, serverParty, (Cmg21jUpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UpsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create default config.
     *
     * @param securityModel security model.
     * @return default config.
     */
    public static UpsiConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Cmg21UpsiConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
