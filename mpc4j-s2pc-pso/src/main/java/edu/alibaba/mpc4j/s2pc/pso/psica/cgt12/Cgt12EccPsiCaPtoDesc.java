package edu.alibaba.mpc4j.s2pc.pso.psica.cgt12;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ECC-based CGT12 PSI Cardinality. This protocol is implicitly introduced in the following paper:
 * <p>
 * Emiliano De Cristofaro, Paolo Gasti, and Gene Tsudik. Fast and private computation of cardinality of set
 * intersection and union. CANS 2012, volume 7712, pages 218–231. Springer, 2012.
 * </p>
 * In Cryptographic Details: Private Preference Matching paragraph.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
class Cgt12EccPsiCaPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7883654564002247205L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CC_PSI_CA";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends H(H(X)^α)
         */
        SERVER_SEND_HASH_HX_ALPHA,
        /**
         * client sends H(Y)^β
         */
        CLIENT_SEND_HY_BETA,
        /**
         * 服务端发送H(Y)^βα
         */
        SERVER_SEND_HY_BETA_ALPHA,
    }

    /**
     * singleton mode
     */
    private static final Cgt12EccPsiCaPtoDesc INSTANCE = new Cgt12EccPsiCaPtoDesc();

    /**
     * private constructor
     */
    private Cgt12EccPsiCaPtoDesc() {
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

