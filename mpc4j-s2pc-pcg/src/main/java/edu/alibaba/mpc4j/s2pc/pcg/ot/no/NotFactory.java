package edu.alibaba.mpc4j.s2pc.pcg.ot.no;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.lh2n.Lh2nNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.lh2n.Lh2nNotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.lh2n.Lh2nNotSender;

/**
 * n选1-OT协议工厂类。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class NotFactory {
    /**
     * 私有构造函数
     */
    private NotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum NotType {
        /**
         * 2^l选1-HOT转换为n选1-OT
         */
        LH2N,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static NotSender createSender(Rpc senderRpc, Party receiverParty, NotConfig config) {
        NotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case LH2N:
                return new Lh2nNotSender(senderRpc, receiverParty, (Lh2nNotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid NotType: " + type.name());
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
    public static NotReceiver createReceiver(Rpc receiverRpc, Party senderParty, NotConfig config) {
        NotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case LH2N:
                return new Lh2nNotReceiver(receiverRpc, senderParty, (Lh2nNotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid NotType: " + type.name());
        }
    }
}
