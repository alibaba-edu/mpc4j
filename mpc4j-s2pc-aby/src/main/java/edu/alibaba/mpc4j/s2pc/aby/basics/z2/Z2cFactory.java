package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cSender;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21.Rrg21Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21.Rrg21Z2cReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21.Rrg21Z2cSender;

/**
 * Z2 circuit factory.
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public class Z2cFactory implements PtoFactory {
    /**
     * private constructor
     */
    private Z2cFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum BcType {
        /**
         * Bea91
         */
        BEA91,
        /**
         * RRG+21
         */
        RRG21,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static Z2cParty createSender(Rpc senderRpc, Party receiverParty, Z2cConfig config) {
        BcType type = config.getPtoType();
        switch (type) {
            case BEA91:
                return new Bea91Z2cSender(senderRpc, receiverParty, (Bea91Z2cConfig) config);
            case RRG21:
                return new Rrg21Z2cSender(senderRpc, receiverParty, (Rrg21Z2cConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BcType.class.getSimpleName() + ": " + type.name());
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
    public static Z2cParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, Z2cConfig config) {
        BcType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BEA91:
                return new Bea91Z2cSender(senderRpc, receiverParty, aiderParty, (Bea91Z2cConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BcType.class.getSimpleName() + ": " + type.name());
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
    public static Z2cParty createReceiver(Rpc receiverRpc, Party senderParty, Z2cConfig config) {
        BcType type = config.getPtoType();
        switch (type) {
            case BEA91:
                return new Bea91Z2cReceiver(receiverRpc, senderParty, (Bea91Z2cConfig) config);
            case RRG21:
                return new Rrg21Z2cReceiver(receiverRpc, senderParty, (Rrg21Z2cConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BcType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param aiderParty  aider party.
     * @param config      config.
     * @return a receiver.
     */
    public static Z2cParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, Z2cConfig config) {
        BcType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BEA91:
                return new Bea91Z2cReceiver(receiverRpc, senderParty, aiderParty, (Bea91Z2cConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BcType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param silent if using a silent protocol.
     * @return a default config.
     */
    public static Z2cConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
                return new Bea91Z2cConfig.Builder(securityModel).build();
            case SEMI_HONEST:
                return new Rrg21Z2cConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }
}
