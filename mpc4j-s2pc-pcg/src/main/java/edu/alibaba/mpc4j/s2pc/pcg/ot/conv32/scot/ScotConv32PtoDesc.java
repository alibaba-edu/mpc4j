package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * F_3 -> F_2 modulus conversion using Silent COT. This protocol comes from Figure 1 from the following paper:
 * <p>
 * Navid Alamati, Guru-Vamsi Policharla, Srinivasan Raghuraman, and Peter Rindal. Improved Alternating Moduli PRFs and
 * Post-Quantum Signatures. To appear in CRYPTO 2024.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
class ScotConv32PtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3888804858550522614L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SCOT_CONV32";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends d
         */
        RECEIVER_SEND_D,
        /**
         * receiver sends d'
         */
        RECEIVER_SEND_D_PRIME,
        /**
         * sender sends (t_0, t_1)
         */
        SENDER_SEND_T0_T1,
    }

    /**
     * singleton mode
     */
    private static final ScotConv32PtoDesc INSTANCE = new ScotConv32PtoDesc();

    /**
     * private constructor
     */
    private ScotConv32PtoDesc() {
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
