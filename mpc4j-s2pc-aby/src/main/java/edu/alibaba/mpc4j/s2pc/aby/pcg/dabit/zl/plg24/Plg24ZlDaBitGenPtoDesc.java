package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.plg24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PRG+24 Zl daBit generation protocol description.
 *
 * @author Weiran Liu
 * @date 2024/7/2
 */
class Plg24ZlDaBitGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5546363883735128564L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PLG24_daBit";

    enum PtoStep {
        /**
         * sender sends correlation
         */
        RECEIVER_SEND_CORRELATION,
    }

    /**
     * singleton mode
     */
    private static final Plg24ZlDaBitGenPtoDesc INSTANCE = new Plg24ZlDaBitGenPtoDesc();

    /**
     * private constructor
     */
    private Plg24ZlDaBitGenPtoDesc() {
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
