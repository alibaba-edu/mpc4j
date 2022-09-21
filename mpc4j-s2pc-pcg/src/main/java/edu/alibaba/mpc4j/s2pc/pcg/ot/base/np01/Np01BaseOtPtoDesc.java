package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * NP01-基础OT协议信息。论文来源：
 * <p>
 * Naor M, Pinkas B. Efficient Oblivious Transfer Protocols.SODA 2001, Society for Industrial and Applied Mathematics,
 * 2001, pp. 448-457.
 * </p>
 * 采用了安全批处理操作，提高批量生成效率。批处理方法来自论文：
 * <p>
 * McQuoid I, Rosulek M, Roy L. Batching base oblivious transfers. ASIACRYPT 2021, Springer, Cham, 2021: 281-310.
 * </p>
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2020/08/12
 */
class Np01BaseOtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6507730682403233384L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "NP01_BASE_OT";

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
    private static final Np01BaseOtPtoDesc INSTANCE = new Np01BaseOtPtoDesc();

    /**
     * 私有构造函数
     */
    private Np01BaseOtPtoDesc() {
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
