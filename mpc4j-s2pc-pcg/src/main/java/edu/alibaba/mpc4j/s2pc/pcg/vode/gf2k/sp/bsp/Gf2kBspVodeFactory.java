package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24.Aprr24Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24.Aprr24Gf2kBspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24.Aprr24Gf2kBspVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.gyw23.Gyw23Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.gyw23.Gyw23Gf2kBspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.gyw23.Gyw23Gf2kBspVodeSender;

/**
 * GF2K-BSP-VODE factory.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Gf2kBspVodeFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kBspVodeFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Gf2kBspVodeType {
        /**
         * GYW23 (semi-honest)
         */
        GYW23,
        /**
         * APRR24 (semi-honest)
         */
        APRR24,
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
    public static int getPrecomputeNum(Gf2kBspVodeConfig config, int subfieldL, int batchNum, int eachNum) {
        Preconditions.checkArgument(
            IntMath.isPowerOfTwo(subfieldL) && subfieldL <= CommonConstants.BLOCK_BIT_LENGTH
        );
        MathPreconditions.checkPositive("batch_um", batchNum);
        MathPreconditions.checkPositive("each_num", eachNum);
        Gf2kBspVodeType type = config.getPtoType();
        switch (type) {
            case APRR24:
            case GYW23:
                return batchNum;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVodeType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kBspVodeSender createSender(Rpc senderRpc, Party receiverParty, Gf2kBspVodeConfig config) {
        Gf2kBspVodeType type = config.getPtoType();
        switch (type) {
            case GYW23:
                return new Gyw23Gf2kBspVodeSender(senderRpc, receiverParty, (Gyw23Gf2kBspVodeConfig) config);
            case APRR24:
                return new Aprr24Gf2kBspVodeSender(senderRpc, receiverParty, (Aprr24Gf2kBspVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVodeType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kBspVodeReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kBspVodeConfig config) {
        Gf2kBspVodeType type = config.getPtoType();
        switch (type) {
            case GYW23:
                return new Gyw23Gf2kBspVodeReceiver(receiverRpc, senderParty, (Gyw23Gf2kBspVodeConfig) config);
            case APRR24:
                return new Aprr24Gf2kBspVodeReceiver(receiverRpc, senderParty, (Aprr24Gf2kBspVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVodeType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static Gf2kBspVodeConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
            case SEMI_HONEST:
                return new Gyw23Gf2kBspVodeConfig.Builder().build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
