package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.gyw23.Gyw23Gf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.gyw23.Gyw23Gf2kBspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.gyw23.Gyw23Gf2kBspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.wykw21.*;

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
         * GYW23
         */
        GYW23,
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
     * @param config    config.
     * @param subfieldL subfield L.
     * @param batchNum  batch num.
     * @param eachNum   each num.
     * @return pre-computed num.
     */
    public static int getPrecomputeNum(Gf2kBspVoleConfig config, int subfieldL, int batchNum, int eachNum) {
        Preconditions.checkArgument(
            IntMath.isPowerOfTwo(subfieldL) && subfieldL <= CommonConstants.BLOCK_BIT_LENGTH
        );
        MathPreconditions.checkPositive("batch_num", batchNum);
        MathPreconditions.checkPositive("each_num", eachNum);
        Gf2kBspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
            case GYW23:
                return batchNum;
            case WYKW21_MALICIOUS:
                return batchNum + CommonConstants.BLOCK_BIT_LENGTH / subfieldL;
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
            case GYW23:
                return new Gyw23Gf2kBspVoleSender(senderRpc, receiverParty, (Gyw23Gf2kBspVoleConfig) config);
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
            case GYW23:
                return new Gyw23Gf2kBspVoleReceiver(receiverRpc, senderParty, (Gyw23Gf2kBspVoleConfig) config);
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
            case TRUSTED_DEALER:
            case SEMI_HONEST:
                return new Gyw23Gf2kBspVoleConfig.Builder().build();
            case MALICIOUS:
                return new Wykw21MaGf2kBspVoleConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
