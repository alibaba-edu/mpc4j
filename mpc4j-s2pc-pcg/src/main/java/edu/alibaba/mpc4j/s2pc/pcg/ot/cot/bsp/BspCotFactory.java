package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.*;

/**
 * BSP-COT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class BspCotFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private BspCotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum BspCotType {
        /**
         * YWL20半诚实安全协议
         */
        YWL20_SEMI_HONEST,
        /**
         * YWL20恶意安全协议
         */
        YWL20_MALICIOUS,
    }

    /**
     * 构建发送方。
     *
     * @param senderRpc     发送方通信接口。
     * @param receiverParty 接收方信息。
     * @param config        配置项。
     * @return 发送方。
     */
    public static BspCotSender createSender(Rpc senderRpc, Party receiverParty, BspCotConfig config) {
        BspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                return new Ywl20ShBspCotSender(senderRpc, receiverParty, (Ywl20ShBspCotConfig) config);
            case YWL20_MALICIOUS:
                return new Ywl20MaBspCotSender(senderRpc, receiverParty, (Ywl20MaBspCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid BspCotType: " + type.name());
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
    public static BspCotReceiver createReceiver(Rpc receiverRpc, Party senderParty, BspCotConfig config) {
        BspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                return new Ywl20ShBspCotReceiver(receiverRpc, senderParty, (Ywl20ShBspCotConfig) config);
            case YWL20_MALICIOUS:
                return new Ywl20MaBspCotReceiver(receiverRpc, senderParty, (Ywl20MaBspCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid BspCotType: " + type.name());
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
    public static int getPrecomputeNum(BspCotConfig config, int batch, int num) {
        assert num > 0 && batch > 0;
        BspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                return LongUtils.ceilLog2(num) * batch;
            case YWL20_MALICIOUS:
                return LongUtils.ceilLog2(num) * batch + CommonConstants.BLOCK_BIT_LENGTH;
            default:
                throw new IllegalArgumentException("Invalid BspCotType: " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static BspCotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Ywl20ShBspCotConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Ywl20MaBspCotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
