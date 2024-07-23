package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23.Gyw23BpCdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23.Gyw23BpCdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23.Gyw23BpCdpprfSender;

/**
 * BP-CDPPRF factory.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class BpCdpprfFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private BpCdpprfFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum BpCdpprfType {
        /**
         * GYW23
         */
        GYW23,
    }

    /**
     * Gets the pre-computed number of COTs.
     *
     * @param config   config.
     * @param batchNum batch num.
     * @param eachNum  each num.
     * @return the pre-computed number of COTs.
     */
    public static int getPrecomputeNum(BpCdpprfConfig config, int batchNum, int eachNum) {
        MathPreconditions.checkPositive("batch_num", batchNum);
        MathPreconditions.checkPositive("each_num", eachNum);
        BpCdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case GYW23:
                return LongUtils.ceilLog2(eachNum, 1) * batchNum;
            default:
                throw new IllegalArgumentException("Invalid " + BpCdpprfType.class.getSimpleName() + ": " + type.name());
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
    public static BpCdpprfSender createSender(Rpc senderRpc, Party receiverParty, BpCdpprfConfig config) {
        BpCdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case GYW23:
                return new Gyw23BpCdpprfSender(senderRpc, receiverParty, (Gyw23BpCdpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BpCdpprfType.class.getSimpleName() + ": " + type.name());
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
    public static BpCdpprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, BpCdpprfConfig config) {
        BpCdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case GYW23:
                return new Gyw23BpCdpprfReceiver(receiverRpc, senderParty, (Gyw23BpCdpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BpCdpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static BpCdpprfConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Gyw23BpCdpprfConfig.Builder().build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
