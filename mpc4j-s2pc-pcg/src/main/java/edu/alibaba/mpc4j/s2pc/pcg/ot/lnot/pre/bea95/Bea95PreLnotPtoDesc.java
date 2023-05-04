package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Bea95 pre-compute 1-out-of-n (with n = 2^l) OT protocol description. The protocol generalizes from the following paper:
 * <p>
 * Beaver, Donald. Precomputing oblivious transfer. CRYPTO 1995, pp. 97-109. Springer, Berlin, Heidelberg, 1995.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
class Bea95PreLnotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7962834820617161749L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BEA95_PRE_LNOT";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * the receiver sends Δ
         */
        RECEIVER_SEND_DELTA,
    }

    /**
     * singleton mode
     */
    private static final Bea95PreLnotPtoDesc INSTANCE = new Bea95PreLnotPtoDesc();

    /**
     * private constructor
     */
    private Bea95PreLnotPtoDesc() {
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
