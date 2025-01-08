package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.ccot;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * F_3 -> F_2 modulus conversion using Core COT. This protocol comes from Figure 1 from the following paper:
 * <p>
 * Navid Alamati, Guru-Vamsi Policharla, Srinivasan Raghuraman, and Peter Rindal. Improved Alternating Moduli PRFs and
 * Post-Quantum Signatures. To appear in CRYPTO 2024.
 * </p>
 * When using Core COT, we can save about 2n-bit communications by setting y_1 = w_{1,0} and y'_1 = w_{1,1}.
 *
 * @author Weiran Liu
 * @date 2024/10/10
 */
class CcotConv32PtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7061750377312117786L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CCOT_CONV32";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends (t_0, t_1)
         */
        SENDER_SEND_T0_T1,
    }

    /**
     * singleton mode
     */
    private static final CcotConv32PtoDesc INSTANCE = new CcotConv32PtoDesc();

    /**
     * private constructor
     */
    private CcotConv32PtoDesc() {
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
