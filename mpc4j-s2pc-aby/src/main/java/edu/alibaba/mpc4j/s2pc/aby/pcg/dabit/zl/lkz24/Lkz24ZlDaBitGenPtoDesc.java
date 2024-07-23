package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.lkz24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LKZ+24 Zl daBit generation protocol description. The protocol is (implicitly) described in Figure 9 of the
 * following paper:
 * <p>
 * Tianpei Lu, Xin Kang, Bingsheng Zhang, Zhuo Ma, Xiaoyuan Zhang, Yang Liu, and Kui Ren. Efficient 2PC for Constant
 * Round Secure Equality Testing and Comparison, IACR ePrint archive, 2024.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/7/2
 */
class Lkz24ZlDaBitGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 610953581318651334L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LZK24_daBit";

    enum PtoStep {
        /**
         * sender sends ciphertexts
         */
        RECEIVER_SEND_CIPHERTEXTS,
    }

    /**
     * singleton mode
     */
    private static final Lkz24ZlDaBitGenPtoDesc INSTANCE = new Lkz24ZlDaBitGenPtoDesc();

    /**
     * private constructor
     */
    private Lkz24ZlDaBitGenPtoDesc() {
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
