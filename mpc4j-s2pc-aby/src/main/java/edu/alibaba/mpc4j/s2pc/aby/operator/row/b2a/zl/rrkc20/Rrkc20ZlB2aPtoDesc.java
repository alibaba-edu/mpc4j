package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RRKC20 Zl boolean to arithmetic protocol description. The protocol comes from the following paper:
 * <p>
 * Rathee, Deevashwer, Mayank Rathee, Nishant Kumar, Nishanth Chandran, Divya Gupta, Aseem Rastogi, and Rahul Sharma.
 * CrypTFlow2: Practical 2-party secure inference. CCS 2020, pp. 325-342. 2020.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2024/6/4
 */
public class Rrkc20ZlB2aPtoDesc implements PtoDesc {
    /**
     * protocol id
     */
    private static final int PTO_ID = Math.abs((int) 2441712777465106482L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RRKC20_ZL_B2A";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends encrypted elements
         */
        SENDER_SENDS_ENC_ELEMENTS,
    }

    /**
     * singleton mode
     */
    private static final Rrkc20ZlB2aPtoDesc INSTANCE = new Rrkc20ZlB2aPtoDesc();

    /**
     * private constructor
     */
    private Rrkc20ZlB2aPtoDesc() {
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
