package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotSender;

/**
 * COT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private CotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum CotType {
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
    public static CotSender createSender(Rpc senderRpc, Party receiverParty, CotConfig config) {
        CotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectCotSender(senderRpc, receiverParty, (DirectCotConfig) config);
            case CACHE:
                return new CacheCotSender(senderRpc, receiverParty, (CacheCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CotType.class.getSimpleName() + ": " + type.name());
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
    public static CotReceiver createReceiver(Rpc receiverRpc, Party senderParty, CotConfig config) {
        CotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectCotReceiver(receiverRpc, senderParty, (DirectCotConfig) config);
            case CACHE:
                return new CacheCotReceiver(receiverRpc, senderParty, (CacheCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static CotConfig createDefaultConfig(SecurityModel securityModel) {
        return new DirectCotConfig.Builder(securityModel).build();
    }
}
