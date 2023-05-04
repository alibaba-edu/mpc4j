package edu.alibaba.mpc4j.s2pc.opf.osn.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21-OSN协议信息。方案来自下述论文附录A.3.1：
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 *
 * @author Weiran Liu
 * @date 2022/02/10
 */
class Gmr21OsnPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)4201424476383923706L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "GMR21_OSN";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送输入分享值
         */
        SENDER_SEND_INPUT_CORRECTIONS,
        /**
         * 发送方发送交换门纠正值
         */
        SENDER_SEND_SWITCH_CORRECTIONS,
    }

    /**
     * 单例模式
     */
    private static final Gmr21OsnPtoDesc INSTANCE = new Gmr21OsnPtoDesc();

    /**
     * 私有构造函数
     */
    private Gmr21OsnPtoDesc() {
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
