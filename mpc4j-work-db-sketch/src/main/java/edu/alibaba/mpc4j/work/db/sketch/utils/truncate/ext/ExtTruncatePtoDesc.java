package edu.alibaba.mpc4j.work.db.sketch.utils.truncate.ext;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Protocol description for the extended (EXT) Truncate implementation.
 * Provides unique identification and naming for the protocol.
 */
public class ExtTruncatePtoDesc implements PtoDesc {
    /**
     * Unique protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3772001954012189298L);
    /**
     * Protocol name
     */
    private static final String PTO_NAME = "TRUNCATE_EXT";

    /**
     * Singleton instance
     */
    private static final ExtTruncatePtoDesc INSTANCE = new ExtTruncatePtoDesc();

    /**
     * Private constructor for singleton pattern
     */
    private ExtTruncatePtoDesc() {
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
