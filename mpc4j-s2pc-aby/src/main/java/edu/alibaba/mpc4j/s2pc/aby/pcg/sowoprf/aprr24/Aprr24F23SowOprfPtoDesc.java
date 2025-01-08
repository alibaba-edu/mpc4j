package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * APRR24 (F2, F3)-sowOPRF protocol description. The protocol comes from the following paper:
 * <p>
 * Navid Alamati, Guru-Vamsi Policharla, Srinivasan Raghuraman, and Peter Rindal. Improved Alternating Moduli PRFs and
 * Post-Quantum Signatures. To appear in CRYPTO 2024.
 * </p>
 * This implementation uses core COT so that we can same some communications.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
class Aprr24F23SowOprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7459774983438553687L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "APRP24_F23_sowOPRF";
    /**
     * the maximum number of input in each batch
     */
    public static final int MAX_BATCH_SIZE = 1 << 21;

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends f
         */
        RECEIVER_SEND_F,
        /**
         * receiver sends δ
         */
        RECEIVER_SEND_DELTA,
        /**
         * sender sends t
         */
        SENDER_SEND_T,
    }

    /**
     * singleton mode
     */
    private static final Aprr24F23SowOprfPtoDesc INSTANCE = new Aprr24F23SowOprfPtoDesc();

    /**
     * private constructor
     */
    private Aprr24F23SowOprfPtoDesc() {
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
