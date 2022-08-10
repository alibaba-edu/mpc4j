package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15.Co15BnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15.Co15BnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15.Co15BnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19BnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19BnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19BnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99.Np99BnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99.Np99BnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99.Np99BnotSender;

/**
 * 基础OT协议工厂类。
 *
 * @author Weiran Liu
 * @date 2021/01/23
 */
public class BnotFactory {
    /**
     * 私有构造函数
     */
    private BnotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum BnotType {
        /**
         * NP99协议
         */
        NP99,
        /**
         * CO15协议
         */
        CO15,
        /**
         * MR19协议
         */
        MR19,
        /**
         * NP01协议
         */
        NP01,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static BnotSender createSender(Rpc senderRpc, Party receiverParty, BnotConfig config) {
        BnotType type = config.getPtoType();
        switch (type) {
            case NP99:
                return new Np99BnotSender(senderRpc, receiverParty, (Np99BnotConfig) config);
            case CO15:
                return new Co15BnotSender(senderRpc, receiverParty, (Co15BnotConfig) config);
            case NP01:
                return new Np01BnotSender(senderRpc, receiverParty, (Np01BnotConfig) config);
            case MR19:
                return new Mr19BnotSender(senderRpc, receiverParty, (Mr19BnotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid BaseOtType: " + type.name());
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
    public static BnotReceiver createReceiver(Rpc receiverRpc, Party senderParty, BnotConfig config) {
        BnotType type = config.getPtoType();
        switch (type) {
            case NP99:
                return new Np99BnotReceiver(receiverRpc, senderParty, (Np99BnotConfig) config);
            case CO15:
                return new Co15BnotReceiver(receiverRpc, senderParty, (Co15BnotConfig) config);
            case NP01:
                return new Np01BnotReceiver(receiverRpc, senderParty, (Np01BnotConfig) config);
            case MR19:
                return new Mr19BnotReceiver(receiverRpc, senderParty, (Mr19BnotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid BaseOtType: " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static BnotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Np99BnotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel.name());
        }
    }
}
