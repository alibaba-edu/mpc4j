package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.quick;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Order select using quick sort.
 */
public class QuickOrderSelectPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4844174494524051368L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "QUICK_ORDER_SELECT";

    /**
     * singleton mode
     */
    private static final QuickOrderSelectPtoDesc INSTANCE = new QuickOrderSelectPtoDesc();

    /**
     * private constructor
     */
    private QuickOrderSelectPtoDesc() {
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
