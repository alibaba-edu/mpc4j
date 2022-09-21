package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * NP01-基础n选1-OT协议信息。论文来源：
 * <p>
 * Naor M, Pinkas B. Efficient Oblivious Transfer Protocols.SODA 2001, Society for Industrial and Applied Mathematics,
 * 2001, pp. 448-457.
 * </p>
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
class Np01BaseNotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 9095730781991975227L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "NP01_BASE_NOT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送初始参数
         */
        SENDER_SEND_INIT,
        /**
         * 接收方发送公钥
         */
        RECEIVER_SEND_PK,
    }

    /**
     * 单例模式
     */
    private static final Np01BaseNotPtoDesc INSTANCE = new Np01BaseNotPtoDesc();

    /**
     * 私有构造函数
     */
    private Np01BaseNotPtoDesc() {
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
