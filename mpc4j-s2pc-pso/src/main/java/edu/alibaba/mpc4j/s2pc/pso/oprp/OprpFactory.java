package edu.alibaba.mpc4j.s2pc.pso.oprp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc.LowMcOprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc.LowMcOprpReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc.LowMcOprpSender;

/**
 * OPRP协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class OprpFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private OprpFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum OprpType {
        /**
         * 20轮LowMC协议
         */
        LOW_MC,
        /**
         * 20轮逆LowMC协议
         */
        LOW_MC_INV,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static OprpSender createSender(Rpc senderRpc, Party receiverParty, OprpConfig config) {
        OprpType type = config.getPtoType();
        switch (type) {
            case LOW_MC:
                return new LowMcOprpSender(senderRpc, receiverParty, (LowMcOprpConfig)config);
            case LOW_MC_INV:
            default:
                throw new IllegalArgumentException("Invalid " + OprpType.class.getSimpleName() + ": " + type.name());
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
    public static OprpReceiver createReceiver(Rpc receiverRpc, Party senderParty, OprpConfig config) {
        OprpType type = config.getPtoType();
        switch (type) {
            case LOW_MC:
                return new LowMcOprpReceiver(receiverRpc, senderParty, (LowMcOprpConfig)config);
            case LOW_MC_INV:
            default:
                throw new IllegalArgumentException("Invalid " + OprpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static OprpConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
                return new LowMcOprpConfig.Builder()
                    .setBcConfig(BcFactory.createDefaultConfig(SecurityModel.IDEAL))
                    .build();
            case SEMI_HONEST:
                return new LowMcOprpConfig.Builder()
                    .setBcConfig(BcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                    .build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
