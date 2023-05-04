package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.ywl20.Ywl20BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.ywl20.Ywl20BpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.ywl20.Ywl20BpDpprfSender;

/**
 * batch-point DPPRF factory.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class BpDpprfFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private BpDpprfFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum BpDpprfType {
        /**
         * YWL20
         */
        YWL20,
    }

    /**
     * Gets the pre-computed number of COTs.
     *
     * @param config     config.
     * @param batchNum   batch num.
     * @param alphaBound α bound.
     * @return the pre-computed number of COTs.
     */
    public static int getPrecomputeNum(BpDpprfConfig config, int batchNum, int alphaBound) {
        assert batchNum > 0 : "BatchNum must be greater than 0: " + batchNum;
        assert alphaBound > 0 : "alphaBound must be greater than 0: " + alphaBound;
        BpDpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return LongUtils.ceilLog2(alphaBound, 1) * batchNum;
            default:
                throw new IllegalArgumentException("Invalid " + BpDpprfType.class.getSimpleName() + ": " + type.name());
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
    public static BpDpprfSender createSender(Rpc senderRpc, Party receiverParty, BpDpprfConfig config) {
        BpDpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20BpDpprfSender(senderRpc, receiverParty, (Ywl20BpDpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BpDpprfType.class.getSimpleName() + ": " + type.name());
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
    public static BpDpprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, BpDpprfConfig config) {
        BpDpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20BpDpprfReceiver(receiverRpc, senderParty, (Ywl20BpDpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BpDpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static BpDpprfConfig createDefaultConfig(SecurityModel securityModel) {
        return new Ywl20BpDpprfConfig.Builder(securityModel).build();
    }
}
