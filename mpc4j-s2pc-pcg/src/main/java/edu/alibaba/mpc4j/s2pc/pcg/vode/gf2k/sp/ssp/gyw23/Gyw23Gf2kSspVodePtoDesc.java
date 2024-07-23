package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GYW23 GF2K-SSP-VODE protodol description.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
class Gyw23Gf2kSspVodePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8723312405727370646L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GYW23_GF2K_SSP_VODE";

    /**
     * private constructor
     */
    private Gyw23Gf2kSspVodePtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends d := s − β ∈ F
         */
        SENDER_SEND_D,
        /**
         * receiver sends (c_1, ..., c_{n-1}, µ, c_n^0, c_n^1, ψ)
         */
        RECEIVER_SEND_CORRELATION,
    }

    /**
     * singleton mode
     */
    private static final Gyw23Gf2kSspVodePtoDesc INSTANCE = new Gyw23Gf2kSspVodePtoDesc();

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
