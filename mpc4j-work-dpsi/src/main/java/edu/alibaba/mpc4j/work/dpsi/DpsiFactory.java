package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.work.dpsi.ccpsi.CcpsiDpsiClient;
import edu.alibaba.mpc4j.work.dpsi.ccpsi.CcpsiDpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.ccpsi.CcpsiDpsiServer;
import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiClient;
import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiServer;

/**
 * DPSI factory.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public class DpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private DpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum DpPsiType {
        /**
         * DP-PSI based on client-payload circuit PSI.
         */
        CCPSI_BASED,
        /**
         * DP-PSI based on mqRPMT.
         */
        MQ_RPMT_BASED,
    }

    /**
     * Creates a server.
     *
     * @param serverRpc   the server RPC.
     * @param clientParty the client party.
     * @param config      the config.
     * @return a server.
     */
    public static <X> DpsiServer<X> createServer(Rpc serverRpc, Party clientParty, DpsiConfig config) {
        DpPsiType type = config.getPtoType();
        switch (type) {
            case CCPSI_BASED:
                return new CcpsiDpsiServer<>(serverRpc, clientParty, (CcpsiDpsiConfig) config);
            case MQ_RPMT_BASED:
                return new MqRpmtDpsiServer<>(serverRpc, clientParty, (MqRpmtDpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + DpPsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a client.
     *
     * @param clientRpc   the client RPC.
     * @param serverParty the server party.
     * @param config      the config.
     * @return a client.
     */
    public static <X> DpsiClient<X> createClient(Rpc clientRpc, Party serverParty, DpsiConfig config) {
        DpPsiType type = config.getPtoType();
        switch (type) {
            case CCPSI_BASED:
                return new CcpsiDpsiClient<>(clientRpc, serverParty, (CcpsiDpsiConfig) config);
            case MQ_RPMT_BASED:
                return new MqRpmtDpsiClient<>(clientRpc, serverParty, (MqRpmtDpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + DpPsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
