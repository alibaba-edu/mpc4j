package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * GF2E-core VOLE factory.
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public class Gf2eCoreVoleFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2eCoreVoleFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum Gf2eCoreVoleType {
        /**
         * KOS16 (semi-honest)
         */
        KOS16,
        /**
         * WYKW21 (malicious)
         */
        WYKW21,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static Gf2eCoreVoleSender createSender(Rpc senderRpc, Party receiverParty, Gf2eCoreVoleConfig config) {
        Gf2eCoreVoleType type = config.getPtoType();
        switch (type) {
            case KOS16:
            case WYKW21:
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eCoreVoleType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2eCoreVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2eCoreVoleConfig config) {
        Gf2eCoreVoleType type = config.getPtoType();
        switch (type) {
            case KOS16:
            case WYKW21:
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eCoreVoleType.class.getSimpleName() + ": " + type.name());
        }
    }
}
