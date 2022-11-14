package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21.Crr21NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21.Crr21NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21.Crr21NcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct.DirectNcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct.DirectNcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct.DirectNcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotSender;

/**
 * NC-COT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/26
 */
public class NcCotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private NcCotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum NcCotType {
        /**
         * 直接协议
         */
        DIRECT,
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
    public static NcCotSender createSender(Rpc senderRpc, Party receiverParty, NcCotConfig config) {
        NcCotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectNcCotSender(senderRpc, receiverParty, (DirectNcCotConfig) config);
            case YWL20:
                return new Ywl20NcCotSender(senderRpc, receiverParty, (Ywl20NcCotConfig)config);
            case CRR21:
                return new Crr21NcCotSender(senderRpc, receiverParty, (Crr21NcCotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + NcCotType.class.getSimpleName() + ": " + type.name());
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
    public static NcCotReceiver createReceiver(Rpc receiverRpc, Party senderParty, NcCotConfig config) {
        NcCotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectNcCotReceiver(receiverRpc, senderParty, (DirectNcCotConfig) config);
            case YWL20:
                return new Ywl20NcCotReceiver(receiverRpc, senderParty, (Ywl20NcCotConfig)config);
            case CRR21:
                return new Crr21NcCotReceiver(receiverRpc, senderParty, (Crr21NcCotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + NcCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static NcCotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Ywl20NcCotConfig.Builder(SecurityModel.SEMI_HONEST).build();
            case COVERT:
            case MALICIOUS:
                return new Ywl20NcCotConfig.Builder(SecurityModel.MALICIOUS).build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
