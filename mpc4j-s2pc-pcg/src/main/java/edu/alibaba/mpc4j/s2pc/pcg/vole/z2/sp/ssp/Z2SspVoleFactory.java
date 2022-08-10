package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.wykw21.Wykw21ShZ2SspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.wykw21.Wykw21ShZ2SspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.wykw21.Wykw21ShZ2SspVoleSender;

/**
 * Z2-SSP-VOLE协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public class Z2SspVoleFactory {
    /**
     * 私有构造函数
     */
    private Z2SspVoleFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum Z2SspVoleType {
        /**
         * WYKW21半诚实安全协议
         */
        WYKW21_SEMI_HONEST,
        /**
         * WYKW21恶意安全协议
         */
        WYKW21_MALICIOUS,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static Z2SspVoleSender createSender(Rpc senderRpc, Party receiverParty, Z2SspVoleConfig config) {
        Z2SspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return new Wykw21ShZ2SspVoleSender(senderRpc, receiverParty, (Wykw21ShZ2SspVoleConfig) config);
            case WYKW21_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid Z2SspVoleType: " + type.name());
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
    public static Z2SspVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Z2SspVoleConfig config) {
        Z2SspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return new Wykw21ShZ2SspVoleReceiver(receiverRpc, senderParty, (Wykw21ShZ2SspVoleConfig) config);
            case WYKW21_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid Z2SspVoleType: " + type.name());
        }
    }

    /**
     * 返回执行协议所需的预计算数量。
     *
     * @param config 配置项。
     * @param num    数量。
     * @return 预计算数量。
     */
    public static int getPrecomputeNum(Z2SspVoleConfig config, int num) {
        assert num > 0;
        Z2SspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return 0;
            case WYKW21_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid Z2SspVoleType: " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static Z2SspVoleConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Wykw21ShZ2SspVoleConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel.name());
        }
    }
}
