package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.ywl20.Ywl20SpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.ywl20.Ywl20SpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.ywl20.Ywl20SpDpprfSender;

/**
 * single-point DPPRF factory.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SpDpprfFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SpDpprfFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum SpDpprfType {
        /**
         * YWL20
         */
        YWL20,
    }

    /**
     * Gets the pre-computed number of COTs.
     *
     * @param config     config.
     * @param alphaBound α bound.
     * @return the pre-computed number of COTs.
     */
    public static int getPrecomputeNum(SpDpprfConfig config, int alphaBound) {
        assert alphaBound > 0 : "alphaBound must be greater than 0: " + alphaBound;
        SpDpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return LongUtils.ceilLog2(alphaBound, 1);
            default:
                throw new IllegalArgumentException("Invalid " + SpDpprfType.class.getSimpleName() + ": " + type.name());
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
    public static SpDpprfSender createSender(Rpc senderRpc, Party receiverParty, SpDpprfConfig config) {
        SpDpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20SpDpprfSender(senderRpc, receiverParty, (Ywl20SpDpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SpDpprfType.class.getSimpleName() + ": " + type.name());
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
    public static SpDpprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, SpDpprfConfig config) {
        SpDpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20SpDpprfReceiver(receiverRpc, senderParty, (Ywl20SpDpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SpDpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static SpDpprfConfig createDefaultConfig(SecurityModel securityModel) {
        return new Ywl20SpDpprfConfig.Builder(securityModel).build();
    }
}
