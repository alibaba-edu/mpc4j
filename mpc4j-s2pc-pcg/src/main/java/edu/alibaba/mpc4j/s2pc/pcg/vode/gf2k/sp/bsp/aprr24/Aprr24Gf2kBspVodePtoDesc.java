package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * APRR24 GF2K-BSP-VODE protocol description.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
class Aprr24Gf2kBspVodePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6657318636049625410L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "APRR24_GF2K_BSP_VODE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends a'
         */
        SENDER_SENDS_A_PRIME_ARRAY,
        /**
         * receiver sends d = γ - Σ_{i ∈ [0, n)} v[i]
         */
        RECEIVER_SEND_DS,
    }

    /**
     * singleton mode
     */
    private static final Aprr24Gf2kBspVodePtoDesc INSTANCE = new Aprr24Gf2kBspVodePtoDesc();

    /**
     * private constructor
     */
    private Aprr24Gf2kBspVodePtoDesc() {
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
