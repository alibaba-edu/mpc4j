package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20.*;

/**
 * SSP-COT协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class SspCotFactory {
    /**
     * 私有构造函数
     */
    private SspCotFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum SspCotType {
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
    public static SspCotSender createSender(Rpc senderRpc, Party receiverParty, SspCotConfig config) {
        SspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                return new Ywl20ShSspCotSender(senderRpc, receiverParty, (Ywl20ShSspCotConfig) config);
            case YWL20_MALICIOUS:
                return new Ywl20MaSspCotSender(senderRpc, receiverParty, (Ywl20MaSspCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SspCotType.class.getSimpleName() + ": " + type.name());
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
    public static SspCotReceiver createReceiver(Rpc receiverRpc, Party senderParty, SspCotConfig config) {
        SspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                return new Ywl20ShSspCotReceiver(receiverRpc, senderParty, (Ywl20ShSspCotConfig) config);
            case YWL20_MALICIOUS:
                return new Ywl20MaSspCotReceiver(receiverRpc, senderParty, (Ywl20MaSspCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SspCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 返回执行协议所需的预计算数量。
     *
     * @param config 配置项。
     * @param num    数量。
     * @return 预计算数量。
     */
    public static int getPrecomputeNum(SspCotConfig config, int num) {
        assert num > 0;
        SspCotType type = config.getPtoType();
        switch (type) {
            case YWL20_SEMI_HONEST:
                return LongUtils.ceilLog2(num);
            case YWL20_MALICIOUS:
                // 恶意安全协议一共要调用log(num) + 128次COT
                return LongUtils.ceilLog2(num) + CommonConstants.BLOCK_BIT_LENGTH;
            default:
                throw new IllegalArgumentException("Invalid " + SspCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建默认协议配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认协议配置项。
     */
    public static SspCotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Ywl20ShSspCotConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Ywl20MaSspCotConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
