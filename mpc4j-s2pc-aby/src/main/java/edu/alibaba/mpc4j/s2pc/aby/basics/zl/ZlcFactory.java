package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.bea91.Bea91ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.bea91.Bea91ZlcReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.bea91.Bea91ZlcSender;

/**
 * Zl circuit party factory.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class ZlcFactory implements PtoFactory {
    /**
     * private constructor
     */
    private ZlcFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum ZlcType {
        /**
         * Bea91
         */
        BEA91,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlcParty createSender(Rpc senderRpc, Party receiverParty, ZlcConfig config) {
        ZlcType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BEA91:
                return new Bea91ZlcSender(senderRpc, receiverParty, (Bea91ZlcConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlcType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static ZlcParty createReceiver(Rpc receiverRpc, Party senderParty, ZlcConfig config) {
        ZlcType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BEA91:
                return new Bea91ZlcReceiver(receiverRpc, senderParty, (Bea91ZlcConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlcType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @param silent        silent.
     * @return a default config.
     */
    public static ZlcConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Bea91ZlcConfig.Builder(securityModel, silent).build();
    }
}
