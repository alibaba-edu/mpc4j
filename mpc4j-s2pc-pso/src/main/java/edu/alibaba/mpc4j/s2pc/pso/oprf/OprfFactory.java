package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.cm20.Cm20MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprf.cm20.Cm20MpOprfSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.kkrt16.*;
import edu.alibaba.mpc4j.s2pc.pso.oprf.ra17.Ra17MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.ra17.Ra17MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprf.ra17.Ra17MpOprfSender;

/**
 * OPRF协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public class OprfFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private OprfFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum OprfType {
        /**
         * 优化KKRT16协议
         */
        KKRT16_OPT,
        /**
         * 原始KKRT16协议
         */
        KKRT16_ORI,
        /**
         * RA17协议
         */
        RA17,
        /**
         * CM20协议
         */
        CM20,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static OprfSender createOprfSender(Rpc senderRpc, Party receiverParty, OprfConfig config) {
        OprfType type = config.getPtoType();
        switch (type) {
            case RA17:
                return new Ra17MpOprfSender(senderRpc, receiverParty, (Ra17MpOprfConfig)config);
            case KKRT16_ORI:
                return new Kkrt16OriOprfSender(senderRpc, receiverParty, (Kkrt16OriOprfConfig)config);
            case KKRT16_OPT:
                return new Kkrt16OptOprfSender(senderRpc, receiverParty, (Kkrt16OptOprfConfig)config);
            case CM20:
                return new Cm20MpOprfSender(senderRpc, receiverParty, (Cm20MpOprfConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + OprfType.class.getSimpleName() + ": " + type.name());
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
    public static OprfReceiver createOprfReceiver(Rpc receiverRpc, Party senderParty, OprfConfig config) {
        OprfType type = config.getPtoType();
        switch (type) {
            case RA17:
                return new Ra17MpOprfReceiver(receiverRpc, senderParty, (Ra17MpOprfConfig)config);
            case KKRT16_ORI:
                return new Kkrt16OriOprfReceiver(receiverRpc, senderParty, (Kkrt16OriOprfConfig)config);
            case KKRT16_OPT:
                return new Kkrt16OptOprfReceiver(receiverRpc, senderParty, (Kkrt16OptOprfConfig)config);
            case CM20:
                return new Cm20MpOprfReceiver(receiverRpc, senderParty, (Cm20MpOprfConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + OprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static OprfConfig createOprfDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kkrt16OptOprfConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static MpOprfSender createMpOprfSender(Rpc senderRpc, Party receiverParty, MpOprfConfig config) {
        OprfType type = config.getPtoType();
        switch (type) {
            case RA17:
                return new Ra17MpOprfSender(senderRpc, receiverParty, (Ra17MpOprfConfig)config);
            case CM20:
                return new Cm20MpOprfSender(senderRpc, receiverParty, (Cm20MpOprfConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + OprfType.class.getSimpleName() + ": " + type.name());
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
    public static MpOprfReceiver createMpOprfReceiver(Rpc receiverRpc, Party senderParty, MpOprfConfig config) {
        OprfType type = config.getPtoType();
        switch (type) {
            case RA17:
                return new Ra17MpOprfReceiver(receiverRpc, senderParty, (Ra17MpOprfConfig)config);
            case CM20:
                return new Cm20MpOprfReceiver(receiverRpc, senderParty, (Cm20MpOprfConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + OprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static MpOprfConfig createMpOprfDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Cm20MpOprfConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
