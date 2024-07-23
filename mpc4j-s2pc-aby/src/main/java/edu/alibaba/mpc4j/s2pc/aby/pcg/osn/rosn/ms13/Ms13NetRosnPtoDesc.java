package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.ms13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * MS13 Network Random OSN protocol description. The protocol comes from the following paper:
 * <p>
 * Mohassel P, Sadeghian S. How to hide circuits in MPC an efficient framework for private function evaluation.
 * EUROCRYPT 2013, Springer, Berlin, Heidelberg, pp. 557-574.
 * </p>
 * The implementation is based on Appendix A.3, Figure 16, of the following paper:
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
class Ms13NetRosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5249901714701128121L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "MS13_NET_ROSN";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends switch corrections
         */
        SENDER_SEND_SWITCH_CORRECTIONS,
    }

    /**
     * singleton mode
     */
    private static final Ms13NetRosnPtoDesc INSTANCE = new Ms13NetRosnPtoDesc();

    /**
     * private constructor
     */
    private Ms13NetRosnPtoDesc() {
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
