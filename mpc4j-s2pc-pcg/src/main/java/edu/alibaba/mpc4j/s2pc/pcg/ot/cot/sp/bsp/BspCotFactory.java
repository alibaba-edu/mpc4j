package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.gyw23.Gyw23BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.gyw23.Gyw23BspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.gyw23.Gyw23BspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.ywl20.*;

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
        /**
         * GYW23
         */
        GYW23,
    }

    /**
     * Gets the pre-computed num.
     *
     * @param config   config.
     * @param batchNum batch num.
     * @param eachNum  each num.
     * @return pre-computed num.
     */
    public static int getPrecomputeNum(BspCotConfig config, int batchNum, int eachNum) {
        MathPreconditions.checkPositive("batchNum", batchNum);
        MathPreconditions.checkPositive("eachNum", eachNum);
        BspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                Ywl20ShBspCotConfig ywl20ShBspCotConfig = (Ywl20ShBspCotConfig) config;
                return BpRdpprfFactory.getPrecomputeNum(ywl20ShBspCotConfig.getBpDpprfConfig(), batchNum, eachNum);
            case YWL20_MALICIOUS:
                Ywl20MaBspCotConfig ywl20MaBspCotConfig = (Ywl20MaBspCotConfig) config;
                return BpRdpprfFactory.getPrecomputeNum(ywl20MaBspCotConfig.getBpDpprfConfig(), batchNum, eachNum)
                    + CommonConstants.BLOCK_BIT_LENGTH;
            case GYW23:
                return LongUtils.ceilLog2(eachNum, 1) * batchNum;
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
            case GYW23:
                return new Gyw23BspCotSender(senderRpc, receiverParty, (Gyw23BspCotConfig) config);
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
            case GYW23:
                return new Gyw23BspCotReceiver(receiverRpc, senderParty, (Gyw23BspCotConfig) config);
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
                return new Gyw23BspCotConfig.Builder().build();
            case MALICIOUS:
                return new Ywl20MaBspCotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
