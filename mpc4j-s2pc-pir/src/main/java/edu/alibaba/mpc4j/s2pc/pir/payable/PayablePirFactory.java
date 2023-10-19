package edu.alibaba.mpc4j.s2pc.pir.payable;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.payable.zlp23.Zlp23PayablePirClient;
import edu.alibaba.mpc4j.s2pc.pir.payable.zlp23.Zlp23PayablePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.payable.zlp23.Zlp23PayablePirServer;

/**
 * Payable PIR factory.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public class PayablePirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PayablePirFactory() {
        // empty
    }

    /**
     * payable PIR type
     */
    public enum PayablePirType {
        /**
         * ZLP23
         */
        ZLP23,
    }

    /**
     * create payable PIR server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return payable PIR server.
     */
    public static PayablePirServer createServer(Rpc serverRpc, Party clientParty, PayablePirConfig config) {
        PayablePirFactory.PayablePirType type = config.getProType();
        if (type == PayablePirType.ZLP23) {
            return new Zlp23PayablePirServer(serverRpc, clientParty, (Zlp23PayablePirConfig) config);
        }
        throw new IllegalArgumentException(
            "Invalid " + PayablePirType.class.getSimpleName() + ": " + type.name()
        );
    }

    /**
     * create payable PIR client.
     *
     * @param clientRpc   client rpc.
     * @param serverParty server party.
     * @param config      config.
     * @return payable PIR client.
     */
    public static PayablePirClient createClient(Rpc clientRpc, Party serverParty, PayablePirConfig config) {
        PayablePirFactory.PayablePirType type = config.getProType();
        if (type == PayablePirType.ZLP23) {
            return new Zlp23PayablePirClient(clientRpc, serverParty, (Zlp23PayablePirConfig) config);
        }
        throw new IllegalArgumentException(
            "Invalid " + PayablePirType.class.getSimpleName() + ": " + type.name()
        );
    }

    /**
     * create default config.
     *
     * @param securityModel security model.
     * @return default config.
     */
    public static PayablePirConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Zlp23PayablePirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
