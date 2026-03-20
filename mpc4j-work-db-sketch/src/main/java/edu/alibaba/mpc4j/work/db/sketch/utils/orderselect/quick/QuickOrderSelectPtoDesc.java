package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.quick;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Protocol description for quick sort based order select implementation.
 * Provides unique identification and naming for the protocol.
 */
public class QuickOrderSelectPtoDesc implements PtoDesc {
    /**
     * Unique protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4844174494524051368L);
    /**
     * Protocol name
     */
    private static final String PTO_NAME = "QUICK_ORDER_SELECT";

    /**
     * Singleton instance
     */
    private static final QuickOrderSelectPtoDesc INSTANCE = new QuickOrderSelectPtoDesc();

    /**
     * Private constructor for singleton pattern
     */
    private QuickOrderSelectPtoDesc() {
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
