package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15.Co15BaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15.Co15BaseNotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15.Co15BaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19EccBaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19EccBaseNotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19EccBaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19KyberBaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19KyberBaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19KyberBaseNotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BaseNotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99.Np99BaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99.Np99BaseNotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99.Np99BaseNotSender;

/**
 * 基础n选1-OT协议工厂类。
 *
 * @author Weiran Liu
 * @date 2021/01/23
 */
public class BaseNotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private BaseNotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum BaseNotType {
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
        MR19_ECC,
        /**
         * MR19基于KYBER的协议
         */
        MR19_KYBER,
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
    public static BaseNotSender createSender(Rpc senderRpc, Party receiverParty, BaseNotConfig config) {
        BaseNotType type = config.getPtoType();
        switch (type) {
            case NP99:
                return new Np99BaseNotSender(senderRpc, receiverParty, (Np99BaseNotConfig) config);
            case CO15:
                return new Co15BaseNotSender(senderRpc, receiverParty, (Co15BaseNotConfig) config);
            case NP01:
                return new Np01BaseNotSender(senderRpc, receiverParty, (Np01BaseNotConfig) config);
            case MR19_ECC:
                return new Mr19EccBaseNotSender(senderRpc, receiverParty, (Mr19EccBaseNotConfig) config);
            case MR19_KYBER:
                return new Mr19KyberBaseNotSender(senderRpc, receiverParty, (Mr19KyberBaseNotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BaseNotType.class.getSimpleName() + ": " + type.name());
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
    public static BaseNotReceiver createReceiver(Rpc receiverRpc, Party senderParty, BaseNotConfig config) {
        BaseNotType type = config.getPtoType();
        switch (type) {
            case NP99:
                return new Np99BaseNotReceiver(receiverRpc, senderParty, (Np99BaseNotConfig) config);
            case CO15:
                return new Co15BaseNotReceiver(receiverRpc, senderParty, (Co15BaseNotConfig) config);
            case NP01:
                return new Np01BaseNotReceiver(receiverRpc, senderParty, (Np01BaseNotConfig) config);
            case MR19_ECC:
                return new Mr19EccBaseNotReceiver(receiverRpc, senderParty, (Mr19EccBaseNotConfig) config);
            case MR19_KYBER:
                return new Mr19KyberBaseNotReceiver(receiverRpc, senderParty, (Mr19KyberBaseNotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BaseNotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static BaseNotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Np99BaseNotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
