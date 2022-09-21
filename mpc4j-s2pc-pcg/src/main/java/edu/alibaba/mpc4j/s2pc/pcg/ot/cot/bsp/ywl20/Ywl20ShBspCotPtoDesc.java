package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * YWL20-BSP-COT半诚实安全协议信息。论文来源：
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class Ywl20ShBspCotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6366716798346786692L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "YWL20_SH_BSP_COT";

    /**
     * 单例模式
     */
    private static final Ywl20ShBspCotPtoDesc INSTANCE = new Ywl20ShBspCotPtoDesc();

    /**
     * 私有构造函数
     */
    private Ywl20ShBspCotPtoDesc() {
        // empty
    }

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送组合消息C
         */
        SENDER_SEND_CORRELATE,
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
