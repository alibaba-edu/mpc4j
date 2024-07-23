package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * F_3 -> F_2 modulus conversion using Silent VOLE. This protocol comes from Figure 1 from the following paper:
 * <p>
 * Navid Alamati, Guru-Vamsi Policharla, Srinivasan Raghuraman, and Peter Rindal. Improved Alternating Moduli PRFs and
 * Post-Quantum Signatures. To appear in CRYPTO 2024.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
class SvoleConv32PtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3558525018010030772L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SVOLE_CONV32";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends d
         */
        RECEIVER_SEND_D,
        /**
         * sender sends (t_0, t_1)
         */
        SENDER_SEND_T0_T1,
    }

    /**
     * singleton mode
     */
    private static final SvoleConv32PtoDesc INSTANCE = new SvoleConv32PtoDesc();

    /**
     * private constructor
     */
    private SvoleConv32PtoDesc() {
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
