package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * GMR21-mqRPMT协议信息。论文来源：
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
class Gmr21MqRpmtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 8473841492126133075L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "GMR21_mqRPMT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送密钥
         */
        SERVER_SEND_KEYS,
        /**
         * 服务端发送布谷鸟哈希密钥
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * 客户端发送OKVS
         */
        CLIENT_SEND_OKVS,
        /**
         * 服务端发送a'
         */
        SERVER_SEND_A_PRIME_OPRFS,
    }

    /**
     * 单例模式
     */
    private static final Gmr21MqRpmtPtoDesc INSTANCE = new Gmr21MqRpmtPtoDesc();

    /**
     * 私有构造函数
     */
    private Gmr21MqRpmtPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }

    /**
     * 有限域字节长度
     */
    static final int FINITE_FIELD_BYTE_LENGTH = Long.BYTES;

    /**
     * 计算PEQT协议对比字节长度σ + 2 * log_2(binNum)，转换为字节长度。
     *
     * @param binNum 桶数量（β）。
     * @return PEQT协议对比长度。
     */
    static int getPeqtByteLength(int binNum) {
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(2 * (LongUtils.ceilLog2(binNum)));
    }
}
