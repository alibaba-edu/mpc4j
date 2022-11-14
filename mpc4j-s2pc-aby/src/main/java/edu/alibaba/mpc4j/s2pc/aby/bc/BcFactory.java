package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcReceiver;
import edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcSender;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;

/**
 * 布尔电路工厂。
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public class BcFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private BcFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum BcType {
        /**
         * Beaver91协议
         */
        BEA91,
        /**
         * GMW87协议
         */
        GMW87,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static BcParty createSender(Rpc senderRpc, Party receiverParty, BcConfig config) {
        BcType type = config.getPtoType();
        switch (type) {
            case BEA91:
                return new Bea91BcSender(senderRpc, receiverParty, (Bea91BcConfig)config);
            case GMW87:
            default:
                throw new IllegalArgumentException("Invalid BcType: " + type.name());
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
    public static BcParty createReceiver(Rpc receiverRpc, Party senderParty, BcConfig config) {
        BcType type = config.getPtoType();
        switch (type) {
            case BEA91:
                return new Bea91BcReceiver(receiverRpc, senderParty, (Bea91BcConfig)config);
            case GMW87:
            default:
                throw new IllegalArgumentException("Invalid BcType: " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static BcConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
                return new Bea91BcConfig.Builder()
                    .setZ2MtgConfig(Z2MtgFactory.createDefaultConfig(SecurityModel.IDEAL))
                    .build();
            case SEMI_HONEST:
                return new Bea91BcConfig.Builder()
                    .setZ2MtgConfig(Z2MtgFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                    .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
