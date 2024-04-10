package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * TCL23 pm-PEQT from Permute Share and OPRF protocol description.
 * The protocol comes from the construction (Section 5.1, pm-PEQT from Permute + Share and OPRF) of the following paper:
 * <p>
 * Binbin Tu, Yu Chen, Qi Liu, and Cong Zhang.
 * Fast Unbalanced Private Set Union from Fully Homomorphic Encryption. CCS 2023, pp. 2959-2973.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2024/3/5
 */
public class Tcl23PsOprfPmPeqtPtoDesc implements PtoDesc {

    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5712238778701510230L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "TCL23_PMPEQT_PS_OPRF";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * sender send prf
         */
        SENDER_SEND_PRF,
    }

    /**
     * the singleton mode
     */
    private static final Tcl23PsOprfPmPeqtPtoDesc INSTANCE = new Tcl23PsOprfPmPeqtPtoDesc();

    /**
     * private constructor.
     */
    private Tcl23PsOprfPmPeqtPtoDesc() {
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
