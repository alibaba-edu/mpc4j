package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.gyw23.Gyw23SpCdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.gyw23.Gyw23SpCdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.gyw23.Gyw23SpCdpprfSender;

/**
 * single-point CDPPRF factory.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class SpCdpprfFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SpCdpprfFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum SpCdpprfType {
        /**
         * GYW23
         */
        GYW23,
    }

    /**
     * Gets the pre-computed number of COTs.
     *
     * @param config config.
     * @param num    n.
     * @return the pre-computed number of COTs.
     */
    public static int getPrecomputeNum(SpCdpprfConfig config, int num) {
        MathPreconditions.checkPositive("n", num);
        SpCdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case GYW23:
                return LongUtils.ceilLog2(num, 1);
            default:
                throw new IllegalArgumentException("Invalid " + SpCdpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static SpCdpprfSender createSender(Rpc senderRpc, Party receiverParty, SpCdpprfConfig config) {
        SpCdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case GYW23:
                return new Gyw23SpCdpprfSender(senderRpc, receiverParty, (Gyw23SpCdpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SpCdpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static SpCdpprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, SpCdpprfConfig config) {
        SpCdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case GYW23:
                return new Gyw23SpCdpprfReceiver(receiverRpc, senderParty, (Gyw23SpCdpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SpCdpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static SpCdpprfConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Gyw23SpCdpprfConfig.Builder().build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
