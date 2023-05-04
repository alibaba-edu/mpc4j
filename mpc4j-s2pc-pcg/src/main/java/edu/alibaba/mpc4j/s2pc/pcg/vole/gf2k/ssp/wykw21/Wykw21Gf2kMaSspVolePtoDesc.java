package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * WYKW21-SSP-GF2K-VOLE (malicious) description. The protocol comes from:
 * <p>
 * Weng, Chenkai, Kang Yang, Jonathan Katz, and Xiao Wang. Wolverine: fast, scalable, and communication-efficient
 * zero-knowledge proofs for boolean and arithmetic circuits. S&P 2021, pp. 1074-1091. IEEE, 2021.
 * </p>
 * The malicious version requires Consistency check shown in Figure 7.
 *
 * @author Weiran Liu
 * @date 2023/3/18
 */
class Wykw21Gf2kMaSspVolePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3760537957677120267L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "WYKW21_GF2K_MA_SSP_VOLE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends d = γ - Σ_{i ∈ [0, n)} v[i]
         */
        RECEIVER_SEND_D,
        /**
         * sender sends x* = β · χ_α - x
         */
        SENDER_SEND_X_STAR,
        /**
         * receiver sends the commitment of V_B
         */
        RECEIVER_SEND_COMMITMENT,
        /**
         * sender sends V_A
         */
        SENDER_SEND_VA,
        /**
         * receiver sends the secret for opening the commitment of V_B
         */
        RECEIVER_SEND_OPEN_SECRET,
    }

    /**
     * singleton mode
     */
    private static final Wykw21Gf2kMaSspVolePtoDesc INSTANCE = new Wykw21Gf2kMaSspVolePtoDesc();

    /**
     * private constructor
     */
    private Wykw21Gf2kMaSspVolePtoDesc() {
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
