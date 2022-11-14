package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * GF2K-核VOLE协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public class Gf2kCoreVoleFactory implements PtoFactory {
    /**
     * 私有构造函数。
     */
    private Gf2kCoreVoleFactory() {
        // empty
    }

    /**
     * 协议类型。
     */
    public enum Gf2kCoreVoleType {
        /**
         * KOS16半诚实安全协议
         */
        KOS16_SEMI_HONEST,
        /**
         * KOS16恶意安全协议
         */
        KOS16_MALICIOUS,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static Gf2kCoreVoleSender createSender(Rpc senderRpc, Party receiverParty, Gf2kCoreVoleConfig config) {
        Gf2kCoreVoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
            case KOS16_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kCoreVoleType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kCoreVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kCoreVoleConfig config) {
        Gf2kCoreVoleType type = config.getPtoType();
        switch (type) {
            case KOS16_SEMI_HONEST:
            case KOS16_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kCoreVoleType.class.getSimpleName() + ": " + type.name());
        }
    }
}
