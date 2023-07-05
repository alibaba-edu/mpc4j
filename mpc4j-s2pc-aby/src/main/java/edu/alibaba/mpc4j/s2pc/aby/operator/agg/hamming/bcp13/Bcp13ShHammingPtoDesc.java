package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * BCP13半诚实安全汉明距离协议信息。论文来源：
 * <p>
 * Julien Bringer, Hervé Chabanne and Alain Patey. SHADE: Secure HAmming DistancE Computaiton from Oblivious Transfer.
 * FC 2013, pp. 164-176, 2013.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
class Bcp13ShHammingPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6159294510188420043L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "BCP13_HAMMING";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送OT数据
         */
        SENDER_SEND_PAYLOAD,
        /**
         * 接收方发送T
         */
        RECEIVER_SEND_T,
        /**
         * 发送方发送R
         */
        SENDER_SEND_R,
    }

    /**
     * 单例模式
     */
    private static final Bcp13ShHammingPtoDesc INSTANCE = new Bcp13ShHammingPtoDesc();

    /**
     * 私有构造函数
     */
    private Bcp13ShHammingPtoDesc() {
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
