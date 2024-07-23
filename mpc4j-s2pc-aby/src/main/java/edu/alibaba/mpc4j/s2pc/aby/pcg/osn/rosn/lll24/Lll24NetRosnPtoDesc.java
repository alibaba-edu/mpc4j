package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LLL24 Network Random OSN protocol description.
 *
 * @author Weiran Liu
 * @date 2024/5/10
 */
class Lll24NetRosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 706068056086986336L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLL24_NET_ROSN";

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
     * private constructor.
     */
    private Lll24NetRosnPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Lll24NetRosnPtoDesc INSTANCE = new Lll24NetRosnPtoDesc();

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
