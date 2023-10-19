package edu.alibaba.mpc4j.s2pc.pso.payablepsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23.Zlp23PayablePsiClient;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23.Zlp23PayablePsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23.Zlp23PayablePsiServer;

/**
 * Payable PSI factory.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
public class PayablePsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PayablePsiFactory() {
        // empty
    }

    /**
     * Payable PSI type
     */
    public enum PayablePsiType {
        /**
         * ZLP23
         */
        ZLP23,
    }

    /**
     * Creates a Payable PSI server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a PSI server.
     */
    public static <X> PayablePsiServer<X> createServer(Rpc serverRpc, Party clientParty, PayablePsiConfig config) {
        PayablePsiType type = config.getPtoType();
        if (type == PayablePsiType.ZLP23) {
            return new Zlp23PayablePsiServer<>(serverRpc, clientParty, (Zlp23PayablePsiConfig) config);
        }
        throw new IllegalArgumentException("Invalid " + PayablePsiType.class.getSimpleName() + ": " + type.name());
    }

    /**
     * Creates a Payable PSI client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static <X> PayablePsiClient<X> createClient(Rpc clientRpc, Party serverParty, PayablePsiConfig config) {
        PayablePsiType type = config.getPtoType();
        if (type == PayablePsiType.ZLP23) {
            return new Zlp23PayablePsiClient<>(clientRpc, serverParty, (Zlp23PayablePsiConfig) config);
        }
        throw new IllegalArgumentException("Invalid " + PayablePsiType.class.getSimpleName() + ": " + type.name());
    }
}
