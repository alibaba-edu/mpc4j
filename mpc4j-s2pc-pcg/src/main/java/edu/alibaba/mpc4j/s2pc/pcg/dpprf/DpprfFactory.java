package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.ywl20.Ywl20DpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.ywl20.Ywl20DpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.ywl20.Ywl20DpprfSender;

/**
 * DPPRF工厂。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class DpprfFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private DpprfFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum DpprfType {
        /**
         * YWL20协议
         */
        YWL20,
    }

    /**
     * 返回执行协议所需的预计算数量。
     *
     * @param config     配置项。
     * @param batchNum   批处理数量。
     * @param alphaBound α上界。
     * @return 预计算数量。
     */
    public static int getPrecomputeNum(DpprfConfig config, int batchNum, int alphaBound) {
        assert batchNum > 0 : "BatchNum must be greater than 0: " + batchNum;
        assert alphaBound > 0 : "alphaBound must be greater than 0: " + alphaBound;
        DpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return LongUtils.ceilLog2(alphaBound) * batchNum;
            default:
                throw new IllegalArgumentException("Invalid " + DpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static DpprfSender createSender(Rpc senderRpc, Party receiverParty, DpprfConfig config) {
        DpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20DpprfSender(senderRpc, receiverParty, (Ywl20DpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + DpprfType.class.getSimpleName() + ": " + type.name());
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
    public static DpprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, DpprfConfig config) {
        DpprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case YWL20:
                return new Ywl20DpprfReceiver(receiverRpc, senderParty, (Ywl20DpprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + DpprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static DpprfConfig createDefaultConfig(SecurityModel securityModel) {
        return new Ywl20DpprfConfig.Builder(securityModel).build();
    }
}
