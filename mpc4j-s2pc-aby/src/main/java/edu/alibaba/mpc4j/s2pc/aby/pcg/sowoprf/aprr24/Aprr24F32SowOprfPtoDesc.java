package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * APRR24 (F3, F2)-sowOPRF protocol description. The protocol comes from the following paper:
 * <p>
 * Navid Alamati, Guru-Vamsi Policharla, Srinivasan Raghuraman, and Peter Rindal. Improved Alternating Moduli PRFs and
 * Post-Quantum Signatures. To appear in CRYPTO 2024.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
class Aprr24F32SowOprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3155219410353552988L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "APRP24_F32_sowOPRF";
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
    }

    /**
     * singleton mode
     */
    private static final Aprr24F32SowOprfPtoDesc INSTANCE = new Aprr24F32SowOprfPtoDesc();

    /**
     * private constructor
     */
    private Aprr24F32SowOprfPtoDesc() {
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
