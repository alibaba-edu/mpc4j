package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16.Kos16Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16.Kos16Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16.Kos16Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21.Wykw21Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21.Wykw21Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21.Wykw21Gf2kCoreVoleSender;

/**
 * GF2K-core VOLE factory.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
public class Gf2kCoreVoleFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kCoreVoleFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum Gf2kCoreVoleType {
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
    public static Gf2kCoreVoleSender createSender(Rpc senderRpc, Party receiverParty, Gf2kCoreVoleConfig config) {
        Gf2kCoreVoleType type = config.getPtoType();
        switch (type) {
            case KOS16:
                return new Kos16Gf2kCoreVoleSender(senderRpc, receiverParty, (Kos16Gf2kCoreVoleConfig) config);
            case WYKW21:
                return new Wykw21Gf2kCoreVoleSender(senderRpc, receiverParty, (Wykw21Gf2kCoreVoleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kCoreVoleType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kCoreVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kCoreVoleConfig config) {
        Gf2kCoreVoleType type = config.getPtoType();
        switch (type) {
            case KOS16:
                return new Kos16Gf2kCoreVoleReceiver(receiverRpc, senderParty, (Kos16Gf2kCoreVoleConfig) config);
            case WYKW21:
                return new Wykw21Gf2kCoreVoleReceiver(receiverRpc, senderParty, (Wykw21Gf2kCoreVoleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kCoreVoleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates the default config.
     *
     * @param securityModel security model.
     * @return the default config.
     */
    public static Gf2kCoreVoleConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kos16Gf2kCoreVoleConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Wykw21Gf2kCoreVoleConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
