package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21 flat Network Random OSN protocol description
 *
 * @author Feng Han
 * @date 2024/7/31
 */
public class Gmr21FlatNetRosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7331689088387248425L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GMR21_FLAT_NET_ROSN";

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
    private static final Gmr21FlatNetRosnPtoDesc INSTANCE = new Gmr21FlatNetRosnPtoDesc();

    /**
     * private constructor
     */
    private Gmr21FlatNetRosnPtoDesc() {
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
