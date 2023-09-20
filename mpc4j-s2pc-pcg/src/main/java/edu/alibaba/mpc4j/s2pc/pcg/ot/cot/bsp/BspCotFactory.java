package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.*;

/**
 * Batched single-point COT factory.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class BspCotFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private BspCotFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum BspCotType {
        /**
         * YWL20 (semi-honest)
         */
        YWL20_SEMI_HONEST,
        /**
         * YWL20 (malicious)
         */
        YWL20_MALICIOUS,
    }

    /**
     * Gets the pre-computed num.
     *
     * @param config   config.
     * @param batchNum batch num.
     * @param num      num in each SSP-COT.
     * @return pre-computed num.
     */
    public static int getPrecomputeNum(BspCotConfig config, int batchNum, int num) {
        MathPreconditions.checkPositive("batchNum", batchNum);
        MathPreconditions.checkPositive("num", num);
        BspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                return LongUtils.ceilLog2(num) * batchNum;
            case YWL20_MALICIOUS:
                return LongUtils.ceilLog2(num) * batchNum + CommonConstants.BLOCK_BIT_LENGTH;
            default:
                throw new IllegalArgumentException("Invalid: " + BspCotType.class.getSimpleName() + ": " + type.name());
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
    public static BspCotSender createSender(Rpc senderRpc, Party receiverParty, BspCotConfig config) {
        BspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                return new Ywl20ShBspCotSender(senderRpc, receiverParty, (Ywl20ShBspCotConfig) config);
            case YWL20_MALICIOUS:
                return new Ywl20MaBspCotSender(senderRpc, receiverParty, (Ywl20MaBspCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid: " + BspCotType.class.getSimpleName() + ": " + type.name());
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
    public static BspCotReceiver createReceiver(Rpc receiverRpc, Party senderParty, BspCotConfig config) {
        BspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                return new Ywl20ShBspCotReceiver(receiverRpc, senderParty, (Ywl20ShBspCotConfig) config);
            case YWL20_MALICIOUS:
                return new Ywl20MaBspCotReceiver(receiverRpc, senderParty, (Ywl20MaBspCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BspCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @return a default config.
     */
    public static BspCotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Ywl20ShBspCotConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Ywl20MaBspCotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
