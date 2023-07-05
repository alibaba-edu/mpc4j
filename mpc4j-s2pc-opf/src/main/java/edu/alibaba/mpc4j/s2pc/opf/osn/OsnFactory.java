package edu.alibaba.mpc4j.s2pc.opf.osn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.ms13.Ms13OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.ms13.Ms13OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.ms13.Ms13OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnSender;

/**
 * OSN协议工厂。
 * <p>
 * 不经意交换网络（Oblivious Switching Network）协议定义来自于下述论文附录A.3：
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * <p>
 * 不经意交换网络协议：
 * - 发送方输入：待交换位置的原数据；接收方输入：交换网络S。
 * - 发送方输出：秘密分享的交换结果。接收方输出：秘密分享的交换结果。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public class OsnFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private OsnFactory() {
        // empty
    }

    /**
     * OSN协议类型
     */
    public enum OsnType {
        /**
         * MS13协议
         */
        MS13,
        /**
         * GMR21协议
         */
        GMR21,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static OsnSender createSender(Rpc senderRpc, Party receiverParty, OsnConfig config) {
        OsnType type = config.getPtoType();
        switch (type) {
            case MS13:
                return new Ms13OsnSender(senderRpc, receiverParty, (Ms13OsnConfig) config);
            case GMR21:
                return new Gmr21OsnSender(senderRpc, receiverParty, (Gmr21OsnConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OsnType.class.getSimpleName() + ": " + type.name());
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
    public static OsnReceiver createReceiver(Rpc receiverRpc, Party senderParty, OsnConfig config) {
        OsnType type = config.getPtoType();
        switch (type) {
            case MS13:
                return new Ms13OsnReceiver(receiverRpc, senderParty, (Ms13OsnConfig) config);
            case GMR21:
                return new Gmr21OsnReceiver(receiverRpc, senderParty, (Gmr21OsnConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OsnType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent config.
     * @return a default config.
     */
    public static OsnConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Gmr21OsnConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
