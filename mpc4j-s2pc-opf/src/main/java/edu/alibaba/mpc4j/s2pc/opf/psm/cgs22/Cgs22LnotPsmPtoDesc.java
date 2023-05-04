package edu.alibaba.mpc4j.s2pc.opf.psm.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 1-out-of-n (with n = 2^l) OT based PSM. The protocol is described in Fig. 6 of the following paper:
 * <p>
 * Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI With Linear Complexity via Relaxed Batch OPPRF.
 * PETS 2022, pp. 353-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
class Cgs22LnotPsmPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6145521838709539677L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGS22_LNOT_PSM";

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
    private static final Cgs22LnotPsmPtoDesc INSTANCE = new Cgs22LnotPsmPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22LnotPsmPtoDesc() {
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
