package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Pre-computed OSN protocol description.
 *
 * @author Feng Han
 * @date 2024/05/08
 */
class Lll24PosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8039511191376388026L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLL24_POSN";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends masks
         */
        SENDER_SEND_MASK,
        /**
         * receiver sends masked permutation
         */
        RECEIVER_SEND_PERMUTATION,
    }

    /**
     * private constructor.
     */
    private Lll24PosnPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Lll24PosnPtoDesc INSTANCE = new Lll24PosnPtoDesc();

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
