package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotFactory.LotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.kk13.*;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.oos17.Oos17LhotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.oos17.Oos17LhotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.oos17.Oos17LhotSender;

/**
 * 2^l选1-HOT协议工厂类。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
public class LhotFactory {

    /**
     * 私有构造函数
     */
    private LhotFactory() {
        // empty
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static LhotSender createSender(Rpc senderRpc, Party receiverParty, LhotConfig config) {
        LotType type = config.getPtoType();
        switch (type) {
            case KK13_ORI:
                return new Kk13OriLhotSender(senderRpc, receiverParty, (Kk13OriLhotConfig) config);
            case KK13_OPT:
                return new Kk13OptLhotSender(senderRpc, receiverParty, (Kk13OptLhotConfig) config);
            case OOS17:
                return new Oos17LhotSender(senderRpc, receiverParty, (Oos17LhotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid LhotType: " + type.name());
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
    public static LhotReceiver createReceiver(Rpc receiverRpc, Party senderParty, LhotConfig config) {
        LotType type = config.getPtoType();
        switch (type) {
            case KK13_ORI:
                return new Kk13OriLhotReceiver(receiverRpc, senderParty, (Kk13OriLhotConfig) config);
            case KK13_OPT:
                return new Kk13OptLhotReceiver(receiverRpc, senderParty, (Kk13OptLhotConfig) config);
            case OOS17:
                return new Oos17LhotReceiver(receiverRpc, senderParty, (Oos17LhotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid LotType: " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static LhotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kk13OriLhotConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Oos17LhotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel);
        }
    }
}
