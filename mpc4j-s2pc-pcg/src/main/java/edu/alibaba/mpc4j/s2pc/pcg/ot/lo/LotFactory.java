package edu.alibaba.mpc4j.s2pc.pcg.ot.lo;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotFactory;

/**
 * 2^l选1-OT协议工厂类。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class LotFactory {
    /**
     * 私有构造函数
     */
    private LotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum LotType {
        /**
         * KK13原始协议
         */
        KK13_ORI,
        /**
         * KK13优化协议
         */
        KK13_OPT,
        /**
         * OOS17协议
         */
        OOS17,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static LotSender createSender(Rpc senderRpc, Party receiverParty, LotConfig config) {
        LotType type = config.getPtoType();
        switch (type) {
            case KK13_ORI:
            case KK13_OPT:
            case OOS17:
                return LhotFactory.createSender(senderRpc, receiverParty, (LhotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid LotType: " + type.name());
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
    public static LotReceiver createReceiver(Rpc receiverRpc, Party senderParty, LotConfig config) {
        LotType type = config.getPtoType();
        switch (type) {
            case KK13_ORI:
            case KK13_OPT:
            case OOS17:
                return LhotFactory.createReceiver(receiverRpc, senderParty, (LhotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid LotType: " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static LotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return LhotFactory.createDefaultConfig(securityModel);
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel);
        }
    }
}
