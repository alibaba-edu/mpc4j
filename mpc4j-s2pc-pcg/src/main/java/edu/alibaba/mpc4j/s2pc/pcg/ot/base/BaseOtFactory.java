package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20.Csw20BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20.Csw20BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20.Csw20BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19EccBaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19EccBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19EccBaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19KyberBaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19KyberBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19KyberBaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.*;

/**
 * 基础OT协议工厂类。
 *
 * @author Weiran Liu
 * @date 2021/01/23
 */
public class BaseOtFactory implements PtoFactory {
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
         * NP01字节协议
         */
        NP01_BYTE,
        /**
         * NP01协议
         */
        NP01,
        /**
         * CO15协议
         */
        CO15,
        /**
         * MR19椭圆曲线协议
         */
        MR19_ECC,
        /**
         * MR19-Kyber协议
         */
        MR19_KYBER,
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
            case NP01_BYTE:
                return new Np01ByteBaseOtSender(senderRpc, receiverParty, (Np01ByteBaseOtConfig) config);
            case MR19_ECC:
                return new Mr19EccBaseOtSender(senderRpc, receiverParty, (Mr19EccBaseOtConfig) config);
            case MR19_KYBER:
                return new Mr19KyberBaseOtSender(senderRpc, receiverParty, (Mr19KyberBaseOtConfig) config);
            case CO15:
                return new Co15BaseOtSender(senderRpc, receiverParty, (Co15BaseOtConfig) config);
            case NP01:
                return new Np01BaseOtSender(senderRpc, receiverParty, (Np01BaseOtConfig) config);
            case CSW20:
                return new Csw20BaseOtSender(senderRpc, receiverParty, (Csw20BaseOtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BaseOtType.class.getSimpleName() + ": " + type.name());
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
            case NP01_BYTE:
                return new Np01ByteBaseOtReceiver(receiverRpc, senderParty, (Np01ByteBaseOtConfig) config);
            case MR19_ECC:
                return new Mr19EccBaseOtReceiver(receiverRpc, senderParty, (Mr19EccBaseOtConfig) config);
            case MR19_KYBER:
                return new Mr19KyberBaseOtReceiver(receiverRpc, senderParty, (Mr19KyberBaseOtConfig) config);
            case CO15:
                return new Co15BaseOtReceiver(receiverRpc, senderParty, (Co15BaseOtConfig) config);
            case NP01:
                return new Np01BaseOtReceiver(receiverRpc, senderParty, (Np01BaseOtConfig) config);
            case CSW20:
                return new Csw20BaseOtReceiver(receiverRpc, senderParty, (Csw20BaseOtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BaseOtType.class.getSimpleName() + ": " + type.name());
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
                return new Np01ByteBaseOtConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
