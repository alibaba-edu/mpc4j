package edu.alibaba.mpc4j.s2pc.opf.psm.pesm.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 1-out-of-n (with n = 2^l) OT based PESM. The protocol is described in Fig. 6 of the following paper:
 * <p>
 * Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI With Linear Complexity via Relaxed Batch OPPRF.
 * PETS 2022, pp. 353-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
class Cgs22LnotPesmPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 9557978501130065L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGS22_LNOT_PESM";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * the sender sends equality payloads
         */
        SENDER_SEND_EV_ARRAYS,
    }

    /**
     * singleton mode
     */
    private static final Cgs22LnotPesmPtoDesc INSTANCE = new Cgs22LnotPesmPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22LnotPesmPtoDesc() {
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
