package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * NP01-字节基础OT协议信息。论文来源：
 * <p>
 * Naor M, Pinkas B. Efficient Oblivious Transfer Protocols.SODA 2001, Society for Industrial and Applied Mathematics,
 * 2001, pp. 448-457.
 * </p>
 * 本实现采用ByteEcc，并采用了安全批处理操作，提高批量生成效率。批处理方法来自论文：
 * <p>
 * McQuoid I, Rosulek M, Roy L. Batching base oblivious transfers. ASIACRYPT 2021, Springer, Cham, 2021: 281-310.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/24
 */
class Np01ByteBaseOtPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2145519878217053299L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "NP01_BYTE_BASE_OT";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends init params.
         */
        SENDER_SEND_INIT,
        /**
         * receiver sends public key.
         */
        RECEIVER_SEND_PK,
    }

    /**
     * singleton mode
     */
    private static final Np01ByteBaseOtPtoDesc INSTANCE = new Np01ByteBaseOtPtoDesc();

    /**
     * 私有构造函数
     */
    private Np01ByteBaseOtPtoDesc() {
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
