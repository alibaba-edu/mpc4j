package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.aprr24.Aprr24Gf2kSspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.aprr24.Aprr24Gf2kSspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.aprr24.Aprr24Gf2kSspVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.gyw23.Gyw23Gf2kSspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.gyw23.Gyw23Gf2kSspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.gyw23.Gyw23Gf2kSspVodeSender;

/**
 * GF2K-SSP-VODE factory.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Gf2kSspVodeFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kSspVodeFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Gf2kSspVodeType {
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
     * @param config    the config.
     * @param subfieldL subfield L.
     * @param num       num.
     * @return the pre-computed num.
     */
    public static int getPrecomputeNum(Gf2kSspVodeConfig config, int subfieldL, int num) {
        Preconditions.checkArgument(
            IntMath.isPowerOfTwo(subfieldL) && subfieldL <= CommonConstants.BLOCK_BIT_LENGTH
        );
        MathPreconditions.checkPositive("num", num);
        Gf2kSspVodeType type = config.getPtoType();
        switch (type) {
            case APRR24:
            case GYW23:
                return 1;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kSspVodeType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kSspVodeSender createSender(Rpc senderRpc, Party receiverParty, Gf2kSspVodeConfig config) {
        Gf2kSspVodeType type = config.getPtoType();
        switch (type) {
            case GYW23:
                return new Gyw23Gf2kSspVodeSender(senderRpc, receiverParty, (Gyw23Gf2kSspVodeConfig) config);
            case APRR24:
                return new Aprr24Gf2kSspVodeSender(senderRpc, receiverParty, (Aprr24Gf2kSspVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kSspVodeType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kSspVodeReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kSspVodeConfig config) {
        Gf2kSspVodeType type = config.getPtoType();
        switch (type) {
            case GYW23:
                return new Gyw23Gf2kSspVodeReceiver(receiverRpc, senderParty, (Gyw23Gf2kSspVodeConfig) config);
            case APRR24:
                return new Aprr24Gf2kSspVodeReceiver(receiverRpc, senderParty, (Aprr24Gf2kSspVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kSspVodeType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static Gf2kSspVodeConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
            case SEMI_HONEST:
                return new Gyw23Gf2kSspVodeConfig.Builder().build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
