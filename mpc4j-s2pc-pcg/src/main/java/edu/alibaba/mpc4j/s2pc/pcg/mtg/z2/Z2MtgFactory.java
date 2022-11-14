package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache.CacheZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache.CacheZ2MtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache.CacheZ2MtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.file.FileZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.file.FileZ2MtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.file.FileZ2MtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgSender;

/**
 * 布尔三元组生成协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/02/07
 */
public class Z2MtgFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private Z2MtgFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum Z2MtgType {
        /**
         * 文件
         */
        FILE,
        /**
         * 离线
         */
        OFFLINE,
        /**
         * 缓存
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
    public static Z2MtgParty createSender(Rpc senderRpc, Party receiverParty, Z2MtgConfig config) {
        Z2MtgType type = config.getPtoType();
        switch (type) {
            case FILE:
                return new FileZ2MtgSender(senderRpc, receiverParty, (FileZ2MtgConfig) config);
            case OFFLINE:
                return new OfflineZ2MtgSender(senderRpc, receiverParty, (OfflineZ2MtgConfig) config);
            case CACHE:
                return new CacheZ2MtgSender(senderRpc, receiverParty, (CacheZ2MtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2MtgType.class.getSimpleName() + ": " + type.name());
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
    public static Z2MtgParty createReceiver(Rpc receiverRpc, Party senderParty, Z2MtgConfig config) {
        Z2MtgType type = config.getPtoType();
        switch (type) {
            case FILE:
                return new FileZ2MtgReceiver(receiverRpc, senderParty, (FileZ2MtgConfig) config);
            case OFFLINE:
                return new OfflineZ2MtgReceiver(receiverRpc, senderParty, (OfflineZ2MtgConfig) config);
            case CACHE:
                return new CacheZ2MtgReceiver(receiverRpc, senderParty, (CacheZ2MtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2MtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static Z2MtgConfig createDefaultConfig(SecurityModel securityModel) {
        return new CacheZ2MtgConfig.Builder(securityModel).build();
    }
}
