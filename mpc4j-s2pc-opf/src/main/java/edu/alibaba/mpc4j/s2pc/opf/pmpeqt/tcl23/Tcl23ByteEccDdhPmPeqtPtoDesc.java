package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * TCL23 pm-PEQT based on Byte Ecc DDH protocol description.
 * The protocol comes from the construction (Section 5.2, pm-PEQT based on DDH) of the following paper:
 * <p>
 * Binbin Tu, Yu Chen, Qi Liu, and Cong Zhang.
 * Fast Unbalanced Private Set Union from Fully Homomorphic Encryption. CCS 2023, pp. 2959-2973.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2024/3/6
 */
public class Tcl23ByteEccDdhPmPeqtPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6099777767710662838L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "TCL23_PMPEQT_BYTE_ECC_DDH";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * receiver send PRFs
         */
        RECEIVER_SEND_PRF,
        /**
         * sender send permuted PRFs
         */
        SENDER_SEND_PERMUTED_PRF,
    }

    /**
     * the singleton mode
     */
    private static final Tcl23ByteEccDdhPmPeqtPtoDesc INSTANCE = new Tcl23ByteEccDdhPmPeqtPtoDesc();

    /**
     * private constructor.
     */
    private Tcl23ByteEccDdhPmPeqtPtoDesc() {
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