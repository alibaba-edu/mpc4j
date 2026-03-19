package edu.alibaba.mpc4j.work.db.sketch.utils.pop.naive;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * naive pop protocol description.
 */
public class NaivePopPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1548033272495651096L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "POP_NAIVE";

    /**
     * singleton mode
     */
    private static final NaivePopPtoDesc INSTANCE = new NaivePopPtoDesc();

    /**
     * private constructor
     */
    private NaivePopPtoDesc() {
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
