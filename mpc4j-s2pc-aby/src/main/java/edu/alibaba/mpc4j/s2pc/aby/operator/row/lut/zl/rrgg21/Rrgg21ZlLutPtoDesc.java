package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RRGG21 Zl lookup table protocol description. The protocol comes from the following paper:
 * <p>
 * Deevashwer Rathee, Mayank Rathee, Rahul Kranti Kiran Goli, Divya Gupta, Rahul Sharma, Nishanth Chandran and
 * Aseem Rastogi.
 * SIRNN: A Math Library for Secure RNN Inference. IEEE S&P 2021, pp. 1003-1020. 2021.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrgg21ZlLutPtoDesc implements PtoDesc {
    /**
     * protocol id
     */
    private static final int PTO_ID = Math.abs((int) 6456152796265926846L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RRGG21_ZL_LOOKUP_TABLE";

    /**
     * singleton mode
     */
    private static final Rrgg21ZlLutPtoDesc INSTANCE = new Rrgg21ZlLutPtoDesc();

    /**
     * private constructor
     */
    private Rrgg21ZlLutPtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender send encrypted elements
         */
        SENDER_SENDS_ENC_ELEMENTS,
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
