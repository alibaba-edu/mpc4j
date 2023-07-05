package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 private equality test protocol description. The protocol is described in Fig. 6 of the following paper:
 * <p>
 * Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI With Linear Complexity via Relaxed Batch OPPRF.
 * PETS 2022, pp. 353-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
class Cgs22PeqtPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7631840207052425416L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGS22_PEQT";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * the sender sends equality payloads
         */
        SENDER_SEND_EVS,
    }

    /**
     * singleton mode
     */
    private static final Cgs22PeqtPtoDesc INSTANCE = new Cgs22PeqtPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22PeqtPtoDesc() {
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
