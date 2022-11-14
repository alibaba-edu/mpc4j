package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.direct.DirectNcLotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.direct.DirectNcLotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.direct.DirectNcLotSender;

/**
 * NC-2^l选1-OT协议工厂类。
 *
 * @author Hanwen Feng
 * @date 2022/08/16
 */
public class NcLotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private NcLotFactory() {
        // empty
    }
    /**
     * 协议类型
     */
    public enum NcLotType {
        /**
         * 直接NC-LOT
         */
        Direct,
        /**
         * YWL20协议
         */
        YWL20,
        /**
         * CRR21协议
         */
        CRR21,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static NcLotSender createSender(Rpc senderRpc, Party receiverParty, NcLotConfig config) {
        NcLotType type = config.getPtoType();
        switch (type) {
            case Direct:
                return new DirectNcLotSender(senderRpc, receiverParty, (DirectNcLotConfig) config);
            case YWL20:
            case CRR21:
            default:
                throw new IllegalArgumentException("Invalid " + NcLotType.class.getSimpleName() + ": " + type.name());
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
    public static NcLotReceiver createReceiver(Rpc receiverRpc, Party senderParty, NcLotConfig config) {
        NcLotType type = config.getPtoType();
        switch (type) {
            case Direct:
                return new DirectNcLotReceiver(receiverRpc, senderParty,(DirectNcLotConfig) config);
            case YWL20:
            case CRR21:
            default:
                throw new IllegalArgumentException("Invalid " + NcLotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static NcLotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new DirectNcLotConfig.Builder(securityModel).build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }

}
