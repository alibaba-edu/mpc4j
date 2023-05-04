package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgSender;

/**
 * 核zp64乘法三元组生成协议工厂。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Zp64CoreMtgFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private Zp64CoreMtgFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum Zp64CoreMtgType {
        /**
         * RSS19协议
         */
        RSS19,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static Zp64CoreMtgParty createSender(Rpc senderRpc, Party receiverParty, Zp64CoreMtgConfig config) {
        Zp64CoreMtgType type = config.getPtoType();
        if (type == Zp64CoreMtgType.RSS19) {
            return new Rss19Zp64CoreMtgSender(senderRpc, receiverParty, (Rss19Zp64CoreMtgConfig) config);
        }
        throw new IllegalArgumentException("Invalid " + Zp64CoreMtgType.class.getSimpleName() + ": " + type.name());
    }

    /**
     * 构建接收方。
     *
     * @param receiverRpc 接收方通信接口。
     * @param senderParty 发送方信息。
     * @param config      配置项。
     * @return 接收方。
     */
    public static Zp64CoreMtgParty createReceiver(Rpc receiverRpc, Party senderParty, Zp64CoreMtgConfig config) {
        Zp64CoreMtgType type = config.getPtoType();
        if (type == Zp64CoreMtgType.RSS19) {
            return new Rss19Zp64CoreMtgReceiver(receiverRpc, senderParty, (Rss19Zp64CoreMtgConfig) config);
        }
        throw new IllegalArgumentException("Invalid " + Zp64CoreMtgType.class.getSimpleName() + ": " + type.name());
    }

    /**
     * 创建默认配置项。
     *
     * @param securityModel 安全模型。
     * @param l l.
     * @return 默认配置项。
     */
    public static Zp64CoreMtgConfig createDefaultConfig(SecurityModel securityModel, int l) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Rss19Zp64CoreMtgConfig.Builder(l).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
