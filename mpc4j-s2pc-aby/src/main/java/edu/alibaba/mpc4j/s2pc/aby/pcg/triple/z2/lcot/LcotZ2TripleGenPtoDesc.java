package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LCOT Z2 triple generation protocol description. The protocol comes from the following paper:
 * <p>
 * Deevashwer Rathee, Mayank Rathee, Nishant Kumar, Nishanth Chandran, Divya Gupta, Aseem Rastogi and Rahul Sharma.
 * CrypTFlow2: Practical 2-Party Secure Inference.
 * CCS 2020, ACM, 2020, pp. 325-342.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2024/5/27
 */
class LcotZ2TripleGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5671455416812293089L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LCOT_Z2_TRIPLE_GENERATION";

    /**
     * private constructor.
     */
    private LcotZ2TripleGenPtoDesc() {
        // empty
    }

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * sender sends encrypted elements
         */
        SENDER_SEND_ENC_ELEMENTS,
    }

    /**
     * singleton mode
     */
    private static final LcotZ2TripleGenPtoDesc INSTANCE = new LcotZ2TripleGenPtoDesc();

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

    /**
     * l invoked in LCOT
     */
    static final int L = 4;
}
