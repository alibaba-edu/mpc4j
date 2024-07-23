package edu.alibaba.mpc4j.work.payable.pir;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.work.payable.pir.baseline.BaselinePayablePirClient;
import edu.alibaba.mpc4j.work.payable.pir.baseline.BaselinePayablePirConfig;
import edu.alibaba.mpc4j.work.payable.pir.baseline.BaselinePayablePirServer;
import edu.alibaba.mpc4j.work.payable.pir.zlp24.Zlp24PayablePirClient;
import edu.alibaba.mpc4j.work.payable.pir.zlp24.Zlp24PayablePirConfig;
import edu.alibaba.mpc4j.work.payable.pir.zlp24.Zlp24PayablePirServer;

/**
 * Payable PIR factory.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
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
        ZLP24,
        /**
         * BASELINE
         */
        BASELINE,
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
        return switch (type) {
            case ZLP24 -> new Zlp24PayablePirServer(serverRpc, clientParty, (Zlp24PayablePirConfig) config);
            case BASELINE -> new BaselinePayablePirServer(serverRpc, clientParty, (BaselinePayablePirConfig) config);
        };
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
        return switch (type) {
            case ZLP24 -> new Zlp24PayablePirClient(clientRpc, serverParty, (Zlp24PayablePirConfig) config);
            case BASELINE -> new BaselinePayablePirClient(clientRpc, serverParty, (BaselinePayablePirConfig) config);
        };
    }

    /**
     * create default config.
     *
     * @param securityModel security model.
     * @return default config.
     */
    public static PayablePirConfig createDefaultConfig(SecurityModel securityModel) {
        return switch (securityModel) {
            case IDEAL, SEMI_HONEST, MALICIOUS -> new Zlp24PayablePirConfig.Builder().build();
            default -> throw new IllegalArgumentException(
                "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
            );
        };
    }
}