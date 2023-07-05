package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.aid.AidZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.aid.AidZlCoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.*;

/**
 * Zl core multiplication triple generator factory.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlCoreMtgFactory implements PtoFactory {
    /**
     * private constructor
     */
    private ZlCoreMtgFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum ZlCoreMtgType {
        /**
         * aid
         */
        AID,
        /**
         * OT-based DSZ15
         */
        DSZ15_OT,
        /**
         * HE-based DSZ15
         */
        DSZ15_HE,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static ZlCoreMtgParty createSender(Rpc senderRpc, Party receiverParty, ZlCoreMtgConfig config) {
        ZlCoreMtgType type = config.getPtoType();
        switch (type) {
            case DSZ15_OT:
                return new Dsz15OtZlCoreMtgSender(senderRpc, receiverParty, (Dsz15OtZlCoreMtgConfig) config);
            case DSZ15_HE:
                return new Dsz15HeZlCoreMtgSender(senderRpc, receiverParty, (Dsz15HeZlCoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCoreMtgType.class.getSimpleName() + ": " + type.name());
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
    public static ZlCoreMtgParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, ZlCoreMtgConfig config) {
        ZlCoreMtgType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case AID:
                return new AidZlCoreMtgParty(senderRpc, receiverParty, aiderParty, (AidZlCoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCoreMtgType.class.getSimpleName() + ": " + type.name());
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
    public static ZlCoreMtgParty createReceiver(Rpc receiverRpc, Party senderParty, ZlCoreMtgConfig config) {
        ZlCoreMtgType type = config.getPtoType();
        switch (type) {
            case DSZ15_OT:
                return new Dsz15OtZlCoreMtgReceiver(receiverRpc, senderParty, (Dsz15OtZlCoreMtgConfig) config);
            case DSZ15_HE:
                return new Dsz15HeZlCoreMtgReceiver(receiverRpc, senderParty, (Dsz15HeZlCoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCoreMtgType.class.getSimpleName() + ": " + type.name());
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
    public static ZlCoreMtgParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, ZlCoreMtgConfig config) {
        ZlCoreMtgType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case AID:
                return new AidZlCoreMtgParty(receiverRpc, senderParty, aiderParty, (AidZlCoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCoreMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @param zl            Zl instance.
     * @return default config.
     */
    public static ZlCoreMtgConfig createDefaultConfig(SecurityModel securityModel, Zl zl) {
        switch (securityModel) {
            case TRUSTED_DEALER:
                return new AidZlCoreMtgConfig.Builder(zl).build();
            case SEMI_HONEST:
                return new Dsz15OtZlCoreMtgConfig.Builder(zl).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
