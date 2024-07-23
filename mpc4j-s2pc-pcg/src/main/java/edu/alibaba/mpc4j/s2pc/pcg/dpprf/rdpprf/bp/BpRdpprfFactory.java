package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.ywl20.Ywl20BpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.ywl20.Ywl20BpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.ywl20.Ywl20BpRdpprfSender;

/**
 * batch-point RDPPRF factory.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class BpRdpprfFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private BpRdpprfFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum BpRdpprfType {
        /**
         * YWL20
         */
        YWL20,
    }

    /**
     * Gets the pre-computed number of COTs.
     *
     * @param config   config.
     * @param batchNum batch num.
     * @param eachNum  each num.
     * @return the pre-computed number of COTs.
     */
    public static int getPrecomputeNum(BpRdpprfConfig config, int batchNum, int eachNum) {
        MathPreconditions.checkPositive("batch_num", batchNum);
        MathPreconditions.checkPositive("each_num", eachNum);
        BpRdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return LongUtils.ceilLog2(eachNum, 1) * batchNum;
            default:
                throw new IllegalArgumentException("Invalid " + BpRdpprfType.class.getSimpleName() + ": " + type.name());
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
    public static BpRdpprfSender createSender(Rpc senderRpc, Party receiverParty, BpRdpprfConfig config) {
        BpRdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20BpRdpprfSender(senderRpc, receiverParty, (Ywl20BpRdpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BpRdpprfType.class.getSimpleName() + ": " + type.name());
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
    public static BpRdpprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, BpRdpprfConfig config) {
        BpRdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20BpRdpprfReceiver(receiverRpc, senderParty, (Ywl20BpRdpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BpRdpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static BpRdpprfConfig createDefaultConfig(SecurityModel securityModel) {
        return new Ywl20BpRdpprfConfig.Builder(securityModel).build();
    }
}
