package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
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
 * MSP-COT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class MspCotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private MspCotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum MspCotType {
        /**
         * YWL20布谷鸟哈希协议
         */
        YWL20_UNI,
        /**
         * YWL20规则索引值协议
         */
        BCG19_REG,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
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
     * 构建接收方。
     *
     * @param receiverRpc 接收方通信接口。
     * @param senderParty 发送方信息。
     * @param config      配置项。
     * @return 接收方。
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
     * 返回执行协议所需的预计算数量。
     *
     * @param config 配置项。
     * @param t      稀疏点数量。
     * @param num    数量。
     * @return 预计算数量。
     */
    public static int getPrecomputeNum(MspCotConfig config, int t, int num) {
        assert num > 0 && t > 0;
        MspCotType type = config.getPtoType();
        switch (type) {
            case BCG19_REG:
                BspCotConfig bcg19BspCotConfig = ((Bcg19RegMspCotConfig) config).getBspCotConfig();
                return BspCotFactory.getPrecomputeNum(bcg19BspCotConfig, t, (int) Math.ceil((double) num / t));
            case YWL20_UNI:
                BspCotConfig ywl20BspCotConfig = ((Ywl20UniMspCotConfig) config).getBspCotConfig();
                int binNum = IntCuckooHashBinFactory.getBinNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, t);
                int keyNum = IntCuckooHashBinFactory.getHashNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE);
                int maxBinSize = MaxBinSizeUtils.expectMaxBinSize(keyNum * num, binNum);
                return BspCotFactory.getPrecomputeNum(ywl20BspCotConfig, binNum, maxBinSize + 1);
            default:
                throw new IllegalArgumentException("Invalid " + MspCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
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
