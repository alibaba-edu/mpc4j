package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21.*;

/**
 * Batch single-point GF2K-VOLE factory.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public class Gf2kBspVoleFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kBspVoleFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Gf2kBspVoleType {
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
     * @param config   config.
     * @param batchNum batch num.
     * @param num      num for each GF2K-SSP-VOLE.
     * @return pre-computed num.
     */
    public static int getPrecomputeNum(Gf2kBspVoleConfig config, int batchNum, int num) {
        MathPreconditions.checkPositive("batchNum", batchNum);
        MathPreconditions.checkPositive("num", num);
        Gf2kBspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return batchNum;
            case WYKW21_MALICIOUS:
                return batchNum + 1;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVoleType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kBspVoleSender createSender(Rpc senderRpc, Party receiverParty, Gf2kBspVoleConfig config) {
        Gf2kBspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return new Wykw21ShGf2kBspVoleSender(senderRpc, receiverParty, (Wykw21ShGf2kBspVoleConfig) config);
            case WYKW21_MALICIOUS:
                return new Wykw21MaGf2kBspVoleSender(senderRpc, receiverParty, (Wykw21MaGf2kBspVoleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVoleType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kBspVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kBspVoleConfig config) {
        Gf2kBspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return new Wykw21ShGf2kBspVoleReceiver(receiverRpc, senderParty, (Wykw21ShGf2kBspVoleConfig) config);
            case WYKW21_MALICIOUS:
                return new Wykw21MaGf2kBspVoleReceiver(receiverRpc, senderParty, (Wykw21MaGf2kBspVoleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVoleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static Gf2kBspVoleConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Wykw21ShGf2kBspVoleConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Wykw21MaGf2kBspVoleConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
