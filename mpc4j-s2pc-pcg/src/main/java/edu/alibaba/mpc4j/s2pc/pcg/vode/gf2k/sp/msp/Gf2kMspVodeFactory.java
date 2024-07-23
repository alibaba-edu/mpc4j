package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.bcg19.Bcg19RegGf2kMspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.bcg19.Bcg19RegGf2kMspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.bcg19.Bcg19RegGf2kMspVodeSender;

/**
 * GF2K-MSP-VODE factory.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Gf2kMspVodeFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kMspVodeFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Gf2kMspVodeType {
        /**
         * BCG19 (regular index)
         */
        BCG19_REG,
    }

    /**
     * Gets pre-computed num.
     *
     * @param config config.
     * @param t      sparse num.
     * @param num    num.
     * @return pre-computed num.
     */
    public static int getPrecomputeNum(Gf2kMspVodeConfig config, int subfieldL, int t, int num) {
        Preconditions.checkArgument(
            IntMath.isPowerOfTwo(subfieldL) && subfieldL <= CommonConstants.BLOCK_BIT_LENGTH
        );
        MathPreconditions.checkPositive("num", num);
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        Gf2kMspVodeType type = config.getPtoType();
        Gf2kBspVodeConfig gf2kBspVodeConfig = config.getGf2kBspVodeConfig();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BCG19_REG:
                return Gf2kBspVodeFactory.getPrecomputeNum(gf2kBspVodeConfig, subfieldL, t, (int) Math.ceil((double) num / t));
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kMspVodeType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kMspVodeSender createSender(Rpc senderRpc, Party receiverParty, Gf2kMspVodeConfig config) {
        Gf2kMspVodeType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BCG19_REG:
                return new Bcg19RegGf2kMspVodeSender(senderRpc, receiverParty, (Bcg19RegGf2kMspVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kMspVodeType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kMspVodeReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kMspVodeConfig config) {
        Gf2kMspVodeType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BCG19_REG:
                return new Bcg19RegGf2kMspVodeReceiver(receiverRpc, senderParty, (Bcg19RegGf2kMspVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kMspVodeType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @return a default config.
     */
    public static Gf2kMspVodeConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
            case SEMI_HONEST:
            case MALICIOUS:
                return new Bcg19RegGf2kMspVodeConfig.Builder(securityModel).build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
