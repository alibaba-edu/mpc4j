package edu.alibaba.mpc4j.s2pc.opf.osn.ms13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * MS13-OSN协议信息。论文来源：
 * Mohassel P, Sadeghian S. How to hide circuits in MPC an efficient framework for private function evaluation.
 * EUROCRYPT 2013, Springer, Berlin, Heidelberg, pp. 557-574.
 *
 * 协议描述来自下述论文的附录A.3图16：
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
class Ms13OsnPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)5456301734071231614L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "MS13_OSN";

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
    private static final Ms13OsnPtoDesc INSTANCE = new Ms13OsnPtoDesc();

    /**
     * 私有构造函数
     */
    private Ms13OsnPtoDesc() {
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
