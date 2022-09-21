package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CO15-基础n选1-OT协议信息。论文来源：
 * <p>
 * Chou T, Orlandi C. The simplest protocol for oblivious transfer. LATINCRYPT 2015, Springer, 2015, pp. 40-58.
 * </p>
 *
 * @author Hanwen Feng
 * @date 2022/07/25
 */
class Co15BaseNotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 3482304997988265869L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CO15_BASE_NOT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送参数S
         */
        SENDER_SEND_S,
        /**
         * 接收方发送参数R
         */
        RECEIVER_SEND_R,
    }

    /**
     * 单例模式
     */
    private static final Co15BaseNotPtoDesc INSTANCE = new Co15BaseNotPtoDesc();

    /**
     * 私有构造函数
     */
    private Co15BaseNotPtoDesc() {
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
