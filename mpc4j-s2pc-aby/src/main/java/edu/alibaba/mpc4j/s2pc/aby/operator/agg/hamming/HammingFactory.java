package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13.Bcp13ShHammingConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13.Bcp13ShHammingReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13.Bcp13ShHammingSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * 汉明距离协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
public class HammingFactory {
    /**
     * 私有构造函数
     */
    private HammingFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum HammingType {
        /**
         * 半诚实安全BCP13协议
         */
        BCP13_SEMI_HONEST,
        /**
         * 恶意安全BCP13协议
         */
        BCP13_MALICIOUS,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static HammingParty createSender(Rpc senderRpc, Party receiverParty, HammingConfig config) {
        HammingType type = config.getPtoType();
        switch (type) {
            case BCP13_SEMI_HONEST:
                return new Bcp13ShHammingSender(senderRpc, receiverParty, (Bcp13ShHammingConfig) config);
            case BCP13_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + HammingType.class.getSimpleName() + ": " + type.name());
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
    public static HammingParty createReceiver(Rpc receiverRpc, Party senderParty, HammingConfig config) {
        HammingType type = config.getPtoType();
        switch (type) {
            case BCP13_SEMI_HONEST:
                return new Bcp13ShHammingReceiver(receiverRpc, senderParty, (Bcp13ShHammingConfig) config);
            case BCP13_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + HammingType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent if using a silent protocol.
     * @return a default config.
     */
    public static HammingConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Bcp13ShHammingConfig.Builder()
                    .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent))
                    .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
