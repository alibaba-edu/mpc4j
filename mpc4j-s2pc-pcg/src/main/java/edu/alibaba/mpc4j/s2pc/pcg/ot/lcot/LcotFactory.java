package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13.*;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.oos17.Oos17LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.oos17.Oos17LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.oos17.Oos17LcotSender;

/**
 * 2^l选1-COT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/21
 */
public class LcotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private LcotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum LcotType {
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
    public static LcotSender createSender(Rpc senderRpc, Party receiverParty, LcotConfig config) {
        LcotType type = config.getPtoType();
        switch (type) {
            case KK13_ORI:
                return new Kk13OriLcotSender(senderRpc, receiverParty, (Kk13OriLcotConfig) config);
            case KK13_OPT:
                return new Kk13OptLcotSender(senderRpc, receiverParty, (Kk13OptLcotConfig) config);
            case OOS17:
                return new Oos17LcotSender(senderRpc, receiverParty, (Oos17LcotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + LcotType.class.getSimpleName() + ": " + type.name());
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
    public static LcotReceiver createReceiver(Rpc receiverRpc, Party senderParty, LcotConfig config) {
        LcotType type = config.getPtoType();
        switch (type) {
            case KK13_ORI:
                return new Kk13OriLcotReceiver(receiverRpc, senderParty, (Kk13OriLcotConfig) config);
            case KK13_OPT:
                return new Kk13OptLcotReceiver(receiverRpc, senderParty, (Kk13OptLcotConfig) config);
            case OOS17:
                return new Oos17LcotReceiver(receiverRpc, senderParty, (Oos17LcotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + LcotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static LcotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kk13OptLcotConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Oos17LcotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }
}
