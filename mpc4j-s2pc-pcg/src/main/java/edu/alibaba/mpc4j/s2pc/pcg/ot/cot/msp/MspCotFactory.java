package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotSender;

/**
 * multi single-point COT factory.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class MspCotFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private MspCotFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum MspCotType {
        /**
         * YWL20 (unique index)
         */
        YWL20_UNI,
        /**
         * YWL20 (regular index)
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
    public static int getPrecomputeNum(MspCotConfig config, int t, int num) {
        MathPreconditions.checkPositive("num", num);
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        MspCotType type = config.getPtoType();
        switch (type) {
            case BCG19_REG:
                BspCotConfig bcg19BspCotConfig = config.getBspCotConfig();
                return BspCotFactory.getPrecomputeNum(bcg19BspCotConfig, t, (int) Math.ceil((double) num / t));
            case YWL20_UNI:
                BspCotConfig ywl20BspCotConfig = config.getBspCotConfig();
                int binNum = IntCuckooHashBinFactory.getBinNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, t);
                int keyNum = IntCuckooHashBinFactory.getHashNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE);
                int maxBinSize = MaxBinSizeUtils.expectMaxBinSize(keyNum * num, binNum);
                return BspCotFactory.getPrecomputeNum(ywl20BspCotConfig, binNum, maxBinSize + 1);
            default:
                throw new IllegalArgumentException("Invalid " + MspCotType.class.getSimpleName() + ": " + type.name());
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
    public static MspCotSender createSender(Rpc senderRpc, Party receiverParty, MspCotConfig config) {
        MspCotType type = config.getPtoType();
        switch (type) {
            case BCG19_REG:
                return new Bcg19RegMspCotSender(senderRpc, receiverParty, (Bcg19RegMspCotConfig) config);
            case YWL20_UNI:
                return new Ywl20UniMspCotSender(senderRpc, receiverParty, (Ywl20UniMspCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + MspCotType.class.getSimpleName() + ": " + type.name());
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
    public static MspCotReceiver createReceiver(Rpc receiverRpc, Party senderParty, MspCotConfig config) {
        MspCotType type = config.getPtoType();
        switch (type) {
            case BCG19_REG:
                return new Bcg19RegMspCotReceiver(receiverRpc, senderParty, (Bcg19RegMspCotConfig) config);
            case YWL20_UNI:
                return new Ywl20UniMspCotReceiver(receiverRpc, senderParty, (Ywl20UniMspCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + MspCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @return a default config.
     */
    public static MspCotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build();
            case COVERT:
            case MALICIOUS:
                return new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
