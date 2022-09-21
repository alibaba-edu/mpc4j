package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * YWL20-BSP-COT恶意安全协议信息。论文来源：
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 * 本实现使用论文附录B的Batched Consistency Check技术实现快速一致性检查机制。
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
class Ywl20MaBspCotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 5636166080693023093L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "YWL20_MA_BSP_COT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送随机预言机密钥
         */
        RECEIVER_SEND_RANDOM_ORACLE_KEY,
        /**
         * 发送方发送组合消息C
         */
        SENDER_SEND_CORRELATE,
        /**
         * 接收方发送x'
         */
        RECEIVER_SEND_CHECK_CHOICES,
        /**
         * 发送方发送H'(V)
         */
        SENDER_SEND_HASH_VALUE,
    }

    /**
     * 单例模式
     */
    private static final Ywl20MaBspCotPtoDesc INSTANCE = new Ywl20MaBspCotPtoDesc();

    /**
     * 私有构造函数
     */
    private Ywl20MaBspCotPtoDesc() {
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
}
