package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.aid.AidZ2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.aid.AidZ2CoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13.Alsz13Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13.Alsz13Z2CoreMtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13.Alsz13Z2CoreMtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;

/**
 * 核布尔三元组生成协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class Z2CoreMtgFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private Z2CoreMtgFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum Z2CoreMtgType {
        /**
         * aid
         */
        AID,
        /**
         * ALSZ13协议
         */
        ALSZ13,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static Z2CoreMtgParty createSender(Rpc senderRpc, Party receiverParty, Z2CoreMtgConfig config) {
        Z2CoreMtgType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case ALSZ13:
                return new Alsz13Z2CoreMtgSender(senderRpc, receiverParty, (Alsz13Z2CoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2CoreMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param aiderParty    aider party.
     * @param config        config.
     * @return a sender.
     */
    public static Z2CoreMtgParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, Z2CoreMtgConfig config) {
        Z2CoreMtgType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case AID:
                return new AidZ2CoreMtgParty(senderRpc, receiverParty, aiderParty, (AidZ2CoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2CoreMtgType.class.getSimpleName() + ": " + type.name());
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
    public static Z2CoreMtgParty createReceiver(Rpc receiverRpc, Party senderParty, Z2CoreMtgConfig config) {
        Z2CoreMtgType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case ALSZ13:
                return new Alsz13Z2CoreMtgReceiver(receiverRpc, senderParty, (Alsz13Z2CoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2CoreMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param aiderParty  aider party.
     * @param config      config.
     * @return a receiver.
     */
    public static Z2CoreMtgParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, Z2CoreMtgConfig config) {
        Z2CoreMtgType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case AID:
                return new AidZ2CoreMtgParty(receiverRpc, senderParty, aiderParty, (AidZ2CoreMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2CoreMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static Z2CoreMtgConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case TRUSTED_DEALER:
                return new AidZ2CoreMtgConfig.Builder().build();
            case SEMI_HONEST:
                return new Alsz13Z2CoreMtgConfig.Builder()
                    .setNcCotConfig(NcCotFactory.createDefaultConfig(securityModel, silent))
                    .build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }

    }
}
