package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.wykw21.Wykw21ShZ2BspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.wykw21.Wykw21ShZ2BspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.wykw21.Wykw21ShZ2BspVoleSender;

/**
 * Z2-BSP-VOLE工厂类。
 *
 * @author Weiran Liu
 * @date 2022/6/22
 */
public class Z2BspVoleFactory {
    /**
     * 私有构造函数
     */
    private Z2BspVoleFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum Z2BspVoleType {
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
    public static Z2BspVoleSender createSender(Rpc senderRpc, Party receiverParty, Z2BspVoleConfig config) {
        Z2BspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return new Wykw21ShZ2BspVoleSender(senderRpc, receiverParty, (Wykw21ShZ2BspVoleConfig) config);
            case WYKW21_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid Z2BspVoleType: " + type.name());
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
    public static Z2BspVoleReceiver createReceiver(Rpc receiverRpc, Party senderParty, Z2BspVoleConfig config) {
        Z2BspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return new Wykw21ShZ2BspVoleReceiver(receiverRpc, senderParty, (Wykw21ShZ2BspVoleConfig) config);
            case WYKW21_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid Z2BspVoleType: " + type.name());
        }
    }

    /**
     * 返回执行协议所需的预计算数量。
     *
     * @param config 配置项。
     * @param batch  批处理数量。
     * @param num    数量。
     * @return 预计算数量。
     */
    public static int getPrecomputeNum(Z2BspVoleConfig config, int batch, int num) {
        assert num > 0 && batch > 0;
        Z2BspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return 0;
            case WYKW21_MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid Z2BspVoleType: " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static Z2BspVoleConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel.name());
        }
    }
}
