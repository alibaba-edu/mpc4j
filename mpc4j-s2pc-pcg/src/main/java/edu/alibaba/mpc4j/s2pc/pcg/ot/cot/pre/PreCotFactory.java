package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotSender;

/**
 * 预计算COT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class PreCotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private PreCotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum PreCotType {
        /**
         * Bea95协议
         */
        Bea95,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static PreCotSender createSender(Rpc senderRpc, Party receiverParty, PreCotConfig config) {
        PreCotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea95:
                return new Bea95PreCotSender(senderRpc, receiverParty, (Bea95PreCotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + PreCotType.class.getSimpleName() + ": " + type.name());
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
    public static PreCotReceiver createReceiver(Rpc receiverRpc, Party senderParty, PreCotConfig config) {
        PreCotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea95:
                return new Bea95PreCotReceiver(receiverRpc, senderParty, (Bea95PreCotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + PreCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static PreCotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Bea95PreCotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
