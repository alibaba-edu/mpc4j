package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * APRR24 GF2K-core-VODE protocol description. The protocol comes from the following paper:
 * <p>
 * Navid Alamati, Guru-Vamsi Policharla, Srinivasan Raghuraman, and Peter Rindal. Improved Alternating Moduli PRFs and
 * Post-Quantum Signatures. To appear in CRYPTO 2024.
 * </p>
 * The paper (implicitly) shows that we can do subfield VOLE with mix multiplication by directly treating the subfield
 * element as a field element. This is correct for semi-honest protocols. However, for malicious protocol, it is unknown
 * how to construct efficient verification.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
class Aprr24Gf2kCoreVodePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8785141215154989796L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "APRR24_GF2K_CORE_VODE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends the matrix
         */
        RECEIVER_SEND_MATRIX,
    }

    /**
     * singleton mode
     */
    private static final Aprr24Gf2kCoreVodePtoDesc INSTANCE = new Aprr24Gf2kCoreVodePtoDesc();

    /**
     * private constructor
     */
    private Aprr24Gf2kCoreVodePtoDesc() {
        // empty
    }

    /**
     * Gets the protocol description.
     *
     * @return the protocol description.
     */
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
