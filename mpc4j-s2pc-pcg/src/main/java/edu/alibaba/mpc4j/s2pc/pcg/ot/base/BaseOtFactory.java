package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20.Csw20BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20.Csw20BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20.Csw20BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01BaseOtSender;

/**
 * 基础OT协议工厂类。
 *
 * @author Weiran Liu
 * @date 2021/01/23
 */
public class BaseOtFactory {
    /**
     * 私有构造函数
     */
    private BaseOtFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum BaseOtType {
        /**
         * NP01协议
         */
        NP01,
        /**
         * CO15协议
         */
        CO15,
        /**
         * MR19协议
         */
        MR19,
        /**
         * CSW20协议
         */
        CSW20,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static BaseOtSender createSender(Rpc senderRpc, Party receiverParty, BaseOtConfig config) {
        BaseOtType type = config.getPtoType();
        switch (type) {
            case MR19:
                return new Mr19BaseOtSender(senderRpc, receiverParty, (Mr19BaseOtConfig)config);
            case CO15:
                return new Co15BaseOtSender(senderRpc, receiverParty, (Co15BaseOtConfig)config);
            case NP01:
                return new Np01BaseOtSender(senderRpc, receiverParty, (Np01BaseOtConfig)config);
            case CSW20:
                return new Csw20BaseOtSender(senderRpc,receiverParty, (Csw20BaseOtConfig)config);
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
    public static BaseOtReceiver createReceiver(Rpc receiverRpc, Party senderParty, BaseOtConfig config) {
        BaseOtType type = config.getPtoType();
        switch (type) {
            case MR19:
                return new Mr19BaseOtReceiver(receiverRpc, senderParty, (Mr19BaseOtConfig)config);
            case CO15:
                return new Co15BaseOtReceiver(receiverRpc, senderParty, (Co15BaseOtConfig)config);
            case NP01:
                return new Np01BaseOtReceiver(receiverRpc, senderParty, (Np01BaseOtConfig)config);
            case CSW20:
                return new Csw20BaseOtReceiver(receiverRpc, senderParty, (Csw20BaseOtConfig)config);
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
    public static BaseOtConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Np01BaseOtConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel.name());
        }
    }
}
