package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13.Alsz13Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13.Alsz13Z2CoreMtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13.Alsz13Z2CoreMtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.ideal.IdealZ2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.ideal.IdealZ2CoreMtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.ideal.IdealZ2CoreMtgSender;

/**
 * 核布尔三元组生成协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class Z2CoreMtgFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private Z2CoreMtgFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum Z2CoreMtgType {
        /**
         * 理想协议
         */
        IDEAL,
        /**
         * ALSZ13协议
         */
        ALSZ13,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static Z2CoreMtgParty createSender(Rpc senderRpc, Party receiverParty, Z2CoreMtgConfig config) {
        Z2CoreMtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealZ2CoreMtgSender(senderRpc, receiverParty, (IdealZ2CoreMtgConfig) config);
            case ALSZ13:
                return new Alsz13Z2CoreMtgSender(senderRpc, receiverParty, (Alsz13Z2CoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2CoreMtgType.class.getSimpleName() + ": " + type.name());
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
    public static Z2CoreMtgParty createReceiver(Rpc receiverRpc, Party senderParty, Z2CoreMtgConfig config) {
        Z2CoreMtgType type = config.getPtoType();
        switch (type) {
            case IDEAL:
                return new IdealZ2CoreMtgReceiver(receiverRpc, senderParty, (IdealZ2CoreMtgConfig) config);
            case ALSZ13:
                return new Alsz13Z2CoreMtgReceiver(receiverRpc, senderParty, (Alsz13Z2CoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2CoreMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认配置项。
     */
    public static Z2CoreMtgConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
                return new IdealZ2CoreMtgConfig.Builder().build();
            case SEMI_HONEST:
                return new Alsz13Z2CoreMtgConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
