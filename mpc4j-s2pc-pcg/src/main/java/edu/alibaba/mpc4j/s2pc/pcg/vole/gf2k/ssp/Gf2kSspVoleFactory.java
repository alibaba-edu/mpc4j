package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21.*;

/**
 * Single single-point GF2K-VOLE factory.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Gf2kSspVoleFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kSspVoleFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Gf2kSspVoleType {
        /**
         * WYKW21 (semi-honest)
         */
        WYKW21_SEMI_HONEST,
        /**
         * WYKW21 (malicious)
         */
        WYKW21_MALICIOUS,
    }

    /**
     * Gets the pre-computed num.
     *
     * @param config the config.
     * @param num    num.
     * @return the pre-computed num.
     */
    public static int getPrecomputeNum(Gf2kSspVoleConfig config, int num) {
        MathPreconditions.checkPositive("num", num);
        Gf2kSspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return 1;
            case WYKW21_MALICIOUS:
                return 2;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kSspVoleType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kSspVoleSender createSender(Rpc senderRpc, Party receiverParty, Gf2kSspVoleConfig config) {
        Gf2kSspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return new Wykw21ShGf2kSspVoleSender(senderRpc, receiverParty, (Wykw21ShGf2kSspVoleConfig) config);
            case WYKW21_MALICIOUS:
                return new Wykw21MaGf2kSspVoleSender(senderRpc, receiverParty, (Wykw21MaGf2kSspVoleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kSspVoleType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kSspVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kSspVoleConfig config) {
        Gf2kSspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return new Wykw21ShGf2kSspVoleReceiver(receiverRpc, senderParty, (Wykw21ShGf2kSspVoleConfig) config);
            case WYKW21_MALICIOUS:
                return new Wykw21MaGf2kSspVoleReceiver(receiverRpc, senderParty, (Wykw21MaGf2kSspVoleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kSspVoleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static Gf2kSspVoleConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Wykw21ShGf2kSspVoleConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Wykw21MaGf2kSspVoleConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
