package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21 Network Random OSN protocol description. The protocol comes from Appendix A.3.1 of the following paper:
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
class Gmr21NetRosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6240572537059667333L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GMR21_NET_ROSN";

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
    private static final Gmr21NetRosnPtoDesc INSTANCE = new Gmr21NetRosnPtoDesc();

    /**
     * private constructor
     */
    private Gmr21NetRosnPtoDesc() {
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
