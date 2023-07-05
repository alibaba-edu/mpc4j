package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.egk20.Egk20ZlEdaBitGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.egk20.Egk20ZlEdaBitGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.egk20.Egk20ZlEdaBitGenSender;

/**
 * Zl edaBit generation factory.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class ZlEdaBitGenFactory implements PtoFactory {
    /**
     * private constructor
     */
    private ZlEdaBitGenFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum ZlEdaBitGenType {
        /**
         * EGK20
         */
        EGK20,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlEdaBitGenParty createSender(Rpc senderRpc, Party receiverParty, ZlEdaBitGenConfig config) {
        ZlEdaBitGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case EGK20:
                return new Egk20ZlEdaBitGenSender(senderRpc, receiverParty, (Egk20ZlEdaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlEdaBitGenType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param aiderParty    aider party.
     * @param config        config.
     * @return a sender.
     */
    public static ZlEdaBitGenParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, ZlEdaBitGenConfig config) {
        ZlEdaBitGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case EGK20:
                return new Egk20ZlEdaBitGenSender(senderRpc, receiverParty, aiderParty, (Egk20ZlEdaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlEdaBitGenType.class.getSimpleName() + ": " + type.name());
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
    public static ZlEdaBitGenParty createReceiver(Rpc receiverRpc, Party senderParty, ZlEdaBitGenConfig config) {
        ZlEdaBitGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case EGK20:
                return new Egk20ZlEdaBitGenReceiver(receiverRpc, senderParty, (Egk20ZlEdaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlEdaBitGenType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param aiderParty aider party.
     * @param config      config.
     * @return a receiver.
     */
    public static ZlEdaBitGenParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, ZlEdaBitGenConfig config) {
        ZlEdaBitGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case EGK20:
                return new Egk20ZlEdaBitGenReceiver(receiverRpc, senderParty, aiderParty, (Egk20ZlEdaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlEdaBitGenType.class.getSimpleName() + ": " + type.name());
        }
    }
}
