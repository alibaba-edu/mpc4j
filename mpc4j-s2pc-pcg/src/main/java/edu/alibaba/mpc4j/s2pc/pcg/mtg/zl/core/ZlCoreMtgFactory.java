package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15ZlCoreMtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15ZlCoreMtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal.IdealZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal.IdealZlCoreMtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal.IdealZlCoreMtgSender;

/**
 * 核l比特三元组生成协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlCoreMtgFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private ZlCoreMtgFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum ZlCoreMtgType {
        /**
         * 理想协议
         */
        IDEAL,
        /**
         * DSZ15协议
         */
        DSZ15,
        /**
         * KOS16半诚实安全协议
         */
        KOS16_SEMI_HONEST,
        /**
         * KOS16恶意安全协议
         */
        KOS16_MALICIOUS,
        /**
         * MZ17协议
         */
        MZ17,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static ZlCoreMtgParty createSender(Rpc senderRpc, Party receiverParty, ZlCoreMtgConfig config) {
        ZlCoreMtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealZlCoreMtgSender(senderRpc, receiverParty, (IdealZlCoreMtgConfig) config);
            case DSZ15:
                return new Dsz15ZlCoreMtgSender(senderRpc, receiverParty, (Dsz15ZlCoreMtgConfig) config);
            case KOS16_SEMI_HONEST:
            case KOS16_MALICIOUS:
            case MZ17:
            default:
                throw new IllegalArgumentException("Invalid " + ZlCoreMtgType.class.getSimpleName() + ": " + type.name());
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
    public static ZlCoreMtgParty createReceiver(Rpc receiverRpc, Party senderParty, ZlCoreMtgConfig config) {
        ZlCoreMtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealZlCoreMtgReceiver(receiverRpc, senderParty, (IdealZlCoreMtgConfig) config);
            case DSZ15:
                return new Dsz15ZlCoreMtgReceiver(receiverRpc, senderParty, (Dsz15ZlCoreMtgConfig) config);
            case KOS16_SEMI_HONEST:
            case KOS16_MALICIOUS:
            case MZ17:
            default:
                throw new IllegalArgumentException("Invalid " + ZlCoreMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认配置项。
     *
     * @param l 乘法三元组比特长度。
     * @param securityModel 安全模型。
     * @return 默认配置项。
     */
    public static ZlCoreMtgConfig createDefaultConfig(SecurityModel securityModel, int l) {
        switch (securityModel) {
            case IDEAL:
                return new IdealZlCoreMtgConfig.Builder(l).build();
            case SEMI_HONEST:
                return new Dsz15ZlCoreMtgConfig.Builder(l).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
