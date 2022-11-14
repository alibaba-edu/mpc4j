package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13.Alsz13CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13.Alsz13CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13.Alsz13CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03.Iknp03CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03.Iknp03CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03.Iknp03CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15.Kos15CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15.Kos15CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15.Kos15CoreCotSender;

/**
 * 核COT协议工厂。
 *
 * @author Weiran Liu
 * @date 2021/01/29
 */
public class CoreCotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private CoreCotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum CoreCotType {
        /**
         * IKNP03协议
         */
        IKNP03,
        /**
         * ALSZ13协议
         */
        ALSZ13,
        /**
         * KOS15协议
         */
        KOS15,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static CoreCotSender createSender(Rpc senderRpc, Party receiverParty, CoreCotConfig config) {
        CoreCotType type = config.getPtoType();
        switch (type) {
            case IKNP03:
                return new Iknp03CoreCotSender(senderRpc, receiverParty, (Iknp03CoreCotConfig) config);
            case ALSZ13:
                return new Alsz13CoreCotSender(senderRpc, receiverParty, (Alsz13CoreCotConfig) config);
            case KOS15:
                return new Kos15CoreCotSender(senderRpc, receiverParty, (Kos15CoreCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CoreCotType.class.getSimpleName() + ": " + type.name());
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
    public static CoreCotReceiver createReceiver(Rpc receiverRpc, Party senderParty, CoreCotConfig config) {
        CoreCotType type = config.getPtoType();
        switch (type) {
            case IKNP03:
                return new Iknp03CoreCotReceiver(receiverRpc, senderParty, (Iknp03CoreCotConfig) config);
            case ALSZ13:
                return new Alsz13CoreCotReceiver(receiverRpc, senderParty, (Alsz13CoreCotConfig) config);
            case KOS15:
                return new Kos15CoreCotReceiver(receiverRpc, senderParty, (Kos15CoreCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CoreCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认配置项。
     */
    public static CoreCotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Alsz13CoreCotConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Kos15CoreCotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
