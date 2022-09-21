package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.kos16;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KOS16半诚实安全核l比特乘法三元组生成协议信息。协议描述来自于下述论文：
 * <p>
 * Keller M, Orsini E, Scholl P. MASCOT: faster malicious arithmetic secure computation with oblivious transfer.
 * CCS 2016. pp. 830-842.
 * </p>
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/9/8
 */
public class Kos16ShZlCoreMtgPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 2708056904590069180L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KOS16_SH_Zl_CORE_MTG";

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
    private static final Kos16ShZlCoreMtgPtoDesc INSTANCE = new Kos16ShZlCoreMtgPtoDesc();

    /**
     * 私有构造函数
     */
    private Kos16ShZlCoreMtgPtoDesc() {
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
