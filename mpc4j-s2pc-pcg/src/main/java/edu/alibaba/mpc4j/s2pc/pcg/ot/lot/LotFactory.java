package edu.alibaba.mpc4j.s2pc.pcg.ot.lot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * 2^l选1-OT协议工厂类。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class LotFactory implements PtoFactory {
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
         * 直接协议
         */
        DIRECT,
        /**
         * 缓存协议
         */
        CACHE,
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
            case DIRECT:
            case CACHE:
            default:
                throw new IllegalArgumentException("Invalid " + LotType.class.getSimpleName() + ": " + type.name());
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
            case DIRECT:
            case CACHE:
            default:
                throw new IllegalArgumentException("Invalid " + LotType.class.getSimpleName() + ": " + type.name());
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
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }
}
