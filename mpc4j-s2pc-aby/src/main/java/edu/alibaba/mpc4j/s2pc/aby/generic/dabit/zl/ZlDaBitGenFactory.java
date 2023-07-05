package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20.*;

/**
 * Zl daBit generation factory.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public class ZlDaBitGenFactory implements PtoFactory {
    /**
     * private constructor
     */
    private ZlDaBitGenFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum ZlDaBitGenType {
        /**
         * EGK20 (no MAC)
         */
        EGK20_NO_MAC,
        /**
         * EGK20 (MAC)
         */
        EGK20_MAC,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlDaBitGenParty createSender(Rpc senderRpc, Party receiverParty, ZlDaBitGenConfig config) {
        ZlDaBitGenType type = config.getPtoType();
        switch (type) {
            case EGK20_NO_MAC:
                return new Egk20NoMacZlDaBitGenSender(senderRpc, receiverParty, (Egk20NoMacZlDaBitGenConfig) config);
            case EGK20_MAC:
                return new Egk20MacZlDaBitGenSender(senderRpc, receiverParty, (Egk20MacZlDaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDaBitGenType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlDaBitGenParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, ZlDaBitGenConfig config) {
        ZlDaBitGenType type = config.getPtoType();
        switch (type) {
            case EGK20_NO_MAC:
                return new Egk20NoMacZlDaBitGenSender(senderRpc, receiverParty, aiderParty, (Egk20NoMacZlDaBitGenConfig) config);
            case EGK20_MAC:
                return new Egk20MacZlDaBitGenSender(senderRpc, receiverParty, aiderParty, (Egk20MacZlDaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDaBitGenType.class.getSimpleName() + ": " + type.name());
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
    public static ZlDaBitGenParty createReceiver(Rpc receiverRpc, Party senderParty, ZlDaBitGenConfig config) {
        ZlDaBitGenType type = config.getPtoType();
        switch (type) {
            case EGK20_NO_MAC:
                return new Egk20NoMacZlDaBitGenReceiver(receiverRpc, senderParty, (Egk20NoMacZlDaBitGenConfig) config);
            case EGK20_MAC:
                return new Egk20MacZlDaBitGenReceiver(receiverRpc, senderParty, (Egk20MacZlDaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDaBitGenType.class.getSimpleName() + ": " + type.name());
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
    public static ZlDaBitGenParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, ZlDaBitGenConfig config) {
        ZlDaBitGenType type = config.getPtoType();
        switch (type) {
            case EGK20_NO_MAC:
                return new Egk20NoMacZlDaBitGenReceiver(receiverRpc, senderParty, aiderParty, (Egk20NoMacZlDaBitGenConfig) config);
            case EGK20_MAC:
                return new Egk20MacZlDaBitGenReceiver(receiverRpc, senderParty, aiderParty, (Egk20MacZlDaBitGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDaBitGenType.class.getSimpleName() + ": " + type.name());
        }
    }
}
