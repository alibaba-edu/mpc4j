package edu.alibaba.mpc4j.s2pc.pso.psica.hfh99;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ECC-based HFH99 PSI Cardinality. This protocol is implicitly introduced in the following paper:
 * <p>
 * Huberman B A, Franklin M, Hogg T. Enhancing privacy and trust in electronic communities. FC 1999, Citeseer, pp. 78-86.
 * </p>
 * In Cryptographic Details: Private Preference Matching paragraph.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
class Hfh99EccPsiCaPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5376138649799085240L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "HFH99_ECC_PSI_CA";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends randomly permuted H(X)^α
         */
        SERVER_SEND_RANDOMLY_PERMUTED_HX_ALPHA,
        /**
         * client sends randomly permuted H(Y)^β
         */
        CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA,
        /**
         * server sends randomly permuted H(Y)^βα
         */
        SERVER_SEND_RANDOMLY_PERMUTED_HY_BETA_ALPHA,
    }

    /**
     * singleton mode
     */
    private static final Hfh99EccPsiCaPtoDesc INSTANCE = new Hfh99EccPsiCaPtoDesc();

    /**
     * private constructor
     */
    private Hfh99EccPsiCaPtoDesc() {
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

