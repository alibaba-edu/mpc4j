package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * DSZ15核l比特三元组生成协议信息。协议描述来自于下述论文第3.A.5节：
 * <p>
 * Daniel Demmler, Thomas Schneider, Michael Zohner: ABY - A Framework for Efficient Mixed-Protocol Secure Two-Party
 * Computation. NDSS 2015
 * </p>
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/9/8
 */
class Dsz15ZlCoreMtgPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 4290799292766578484L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "DSZ15_Zl_CORE_MTG";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送消息
         */
        RECEIVER_SEND_MESSAGES,
        /**
         * 发送方发送消息
         */
        SENDER_SEND_MESSAGES,
    }

    /**
     * 单例模式
     */
    private static final Dsz15ZlCoreMtgPtoDesc INSTANCE = new Dsz15ZlCoreMtgPtoDesc();

    /**
     * 私有构造函数
     */
    private Dsz15ZlCoreMtgPtoDesc() {
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
