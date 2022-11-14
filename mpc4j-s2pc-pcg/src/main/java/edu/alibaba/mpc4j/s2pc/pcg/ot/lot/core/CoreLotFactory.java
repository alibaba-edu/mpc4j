package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.kk13.*;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.oos17.Oos17CoreLotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.oos17.Oos17CoreLotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.oos17.Oos17CoreLotSender;

/**
 * 核2^l选1-OT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/21
 */
public class CoreLotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private CoreLotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum CoreLotType {
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
    public static CoreLotSender createSender(Rpc senderRpc, Party receiverParty, CoreLotConfig config) {
        CoreLotType type = config.getPtoType();
        switch (type) {
            case KK13_ORI:
                return new Kk13OriCoreLotSender(senderRpc, receiverParty, (Kk13OriCoreLotConfig) config);
            case KK13_OPT:
                return new Kk13OptCoreLotSender(senderRpc, receiverParty, (Kk13OptCoreLotConfig) config);
            case OOS17:
                return new Oos17CoreLotSender(senderRpc, receiverParty, (Oos17CoreLotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CoreLotType.class.getSimpleName() + ": " + type.name());
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
    public static CoreLotReceiver createReceiver(Rpc receiverRpc, Party senderParty, CoreLotConfig config) {
        CoreLotType type = config.getPtoType();
        switch (type) {
            case KK13_ORI:
                return new Kk13OriCoreLotReceiver(receiverRpc, senderParty, (Kk13OriCoreLotConfig) config);
            case KK13_OPT:
                return new Kk13OptCoreLotReceiver(receiverRpc, senderParty, (Kk13OptCoreLotConfig) config);
            case OOS17:
                return new Oos17CoreLotReceiver(receiverRpc, senderParty, (Oos17CoreLotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CoreLotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static CoreLotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kk13OriCoreLotConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Oos17CoreLotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }
}
