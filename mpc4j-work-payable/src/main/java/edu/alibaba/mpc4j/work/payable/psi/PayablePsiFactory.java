package edu.alibaba.mpc4j.work.payable.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.work.payable.psi.baseline.BaselinePayablePsiClient;
import edu.alibaba.mpc4j.work.payable.psi.baseline.BaselinePayablePsiConfig;
import edu.alibaba.mpc4j.work.payable.psi.baseline.BaselinePayablePsiServer;
import edu.alibaba.mpc4j.work.payable.psi.zlp24.Zlp24PayablePsiClient;
import edu.alibaba.mpc4j.work.payable.psi.zlp24.Zlp24PayablePsiConfig;
import edu.alibaba.mpc4j.work.payable.psi.zlp24.Zlp24PayablePsiServer;

/**
 * Payable PSI Factory.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
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
         * ZLP24
         */
        ZLP24,
        /**
         *
         */
        BASELINE,
    }

    /**
     * Creates a Payable PSI server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a PSI server.
     */
    public static PayablePsiServer createServer(Rpc serverRpc, Party clientParty, PayablePsiConfig config) {
        PayablePsiType type = config.getPtoType();
        return switch (type) {
            case ZLP24 -> new Zlp24PayablePsiServer(serverRpc, clientParty, (Zlp24PayablePsiConfig) config);
            case BASELINE -> new BaselinePayablePsiServer(serverRpc, clientParty, (BaselinePayablePsiConfig) config);
        };
    }

    /**
     * Creates a Payable PSI client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static PayablePsiClient createClient(Rpc clientRpc, Party serverParty, PayablePsiConfig config) {
        PayablePsiType type = config.getPtoType();
        return switch (type) {
            case ZLP24 -> new Zlp24PayablePsiClient(clientRpc, serverParty, (Zlp24PayablePsiConfig) config);
            case BASELINE -> new BaselinePayablePsiClient(clientRpc, serverParty, (BaselinePayablePsiConfig) config);
        };
    }
}
