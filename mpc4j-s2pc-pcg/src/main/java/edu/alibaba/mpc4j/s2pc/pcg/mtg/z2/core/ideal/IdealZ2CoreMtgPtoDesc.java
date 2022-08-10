package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.ideal;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 理想核布尔三元组生成协议信息。两方共享一个三元组生成种子，后续两方可以生成无穷多个三元组。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
class IdealZ2CoreMtgPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 8772186969238627796L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "IDEAL_Z2_CORE_MTG";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送种子
         */
        SERVER_SEND_ROOT_KEY,
    }

    /**
     * 单例模式
     */
    private static final IdealZ2CoreMtgPtoDesc INSTANCE = new IdealZ2CoreMtgPtoDesc();

    /**
     * 私有构造函数
     */
    private IdealZ2CoreMtgPtoDesc() {
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
