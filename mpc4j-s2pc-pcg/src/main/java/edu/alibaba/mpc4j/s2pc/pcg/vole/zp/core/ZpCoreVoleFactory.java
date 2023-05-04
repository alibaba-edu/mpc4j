package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16.Kos16ZpCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16.Kos16ZpCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16.Kos16ZpCoreVoleReceiver;

/**
 * Zp-核VOLE协议工厂类。
 *
 * @author Hanwen Feng
 * @date 2022/06/07
 */
public class ZpCoreVoleFactory implements PtoFactory {
    /**
     * 私有构造函数。
     */
    private ZpCoreVoleFactory() {
        // empty
    }

    /**
     * 协议类型。
     */
    public enum ZpCoreVoleType {
        /**
         * KOS16 (semi-honest)
         */
        KOS16,
        /**
         * WYKW21 (malicious)
         */
        WYKW21,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static ZpCoreVoleSender createSender(Rpc senderRpc, Party receiverParty, ZpCoreVoleConfig config) {
        ZpCoreVoleType type = config.getPtoType();
        switch (type) {
            case KOS16:
                return new Kos16ZpCoreVoleSender(senderRpc, receiverParty, (Kos16ZpCoreVoleConfig) config);
            case WYKW21:
            default:
                throw new IllegalArgumentException("Invalid " + ZpCoreVoleType.class.getSimpleName() + ": " + type.name());
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
    public static ZpCoreVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, ZpCoreVoleConfig config) {
        ZpCoreVoleType type = config.getPtoType();
        switch (type) {
            case KOS16:
                return new Kos16ZpCoreVoleReceiver(receiverRpc, senderParty, (Kos16ZpCoreVoleConfig) config);
            case WYKW21:
            default:
                throw new IllegalArgumentException("Invalid " + ZpCoreVoleType.class.getSimpleName() + ": " + type.name());
        }
    }
}
