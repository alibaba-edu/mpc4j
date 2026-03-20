package edu.alibaba.mpc4j.work.db.sketch.utils.pop.naive;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Protocol description for the naive Pop (Permute-and-Open) implementation.
 * Provides unique identification and naming for the protocol.
 */
public class NaivePopPtoDesc implements PtoDesc {
    /**
     * Unique protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1548033272495651096L);
    /**
     * Protocol name
     */
    private static final String PTO_NAME = "POP_NAIVE";

    /**
     * Singleton instance
     */
    private static final NaivePopPtoDesc INSTANCE = new NaivePopPtoDesc();

    /**
     * Private constructor for singleton pattern
     */
    private NaivePopPtoDesc() {
        // empty
    }

    /**
     * Get the singleton instance
     *
     * @return the protocol description instance
     */
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
