package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.ywl20.Ywl20SpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.ywl20.Ywl20SpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.ywl20.Ywl20SpRdpprfSender;

/**
 * single-point RDPPRF factory.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SpRdpprfFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SpRdpprfFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum SpRdpprfType {
        /**
         * YWL20
         */
        YWL20,
    }

    /**
     * Gets the pre-computed number of COTs.
     *
     * @param config config.
     * @param num    n.
     * @return the pre-computed number of COTs.
     */
    public static int getPrecomputeNum(SpRdpprfConfig config, int num) {
        MathPreconditions.checkPositive("n", num);
        SpRdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return LongUtils.ceilLog2(num, 1);
            default:
                throw new IllegalArgumentException("Invalid " + SpRdpprfType.class.getSimpleName() + ": " + type.name());
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
    public static SpRdpprfSender createSender(Rpc senderRpc, Party receiverParty, SpRdpprfConfig config) {
        SpRdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20SpRdpprfSender(senderRpc, receiverParty, (Ywl20SpRdpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SpRdpprfType.class.getSimpleName() + ": " + type.name());
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
    public static SpRdpprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, SpRdpprfConfig config) {
        SpRdpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20SpRdpprfReceiver(receiverRpc, senderParty, (Ywl20SpRdpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SpRdpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static SpRdpprfConfig createDefaultConfig(SecurityModel securityModel) {
        return new Ywl20SpRdpprfConfig.Builder(securityModel).build();
    }
}
