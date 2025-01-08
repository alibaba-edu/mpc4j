package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LLL24 flat Network Random OSN protocol description.
 *
 * @author Feng Han
 * @date 2024/7/29
 */
public class Lll24FlatNetRosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -2458992902298960100L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLL24_FLAT_NET_ROSN";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends switch corrections
         */
        SENDER_SEND_SWITCH_CORRECTIONS,
        /**
         * sender sends switch corrections
         */
        SYNCHRONIZE_MSG,
    }

    /**
     * private constructor.
     */
    private Lll24FlatNetRosnPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Lll24FlatNetRosnPtoDesc INSTANCE = new Lll24FlatNetRosnPtoDesc();

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
