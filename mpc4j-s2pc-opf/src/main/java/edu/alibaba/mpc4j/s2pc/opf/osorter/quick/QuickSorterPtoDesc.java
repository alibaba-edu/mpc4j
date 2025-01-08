package edu.alibaba.mpc4j.s2pc.opf.osorter.quick;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * @author Feng Han
 * @date 2024/9/27
 */
public class QuickSorterPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -2281400392971718513L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "QUICK_SORTER";

    /**
     * the singleton mode
     */
    private static final QuickSorterPtoDesc INSTANCE = new QuickSorterPtoDesc();

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * parties share seed
         */
        SHARE_SEED,
        /**
         * reveal the comparison result
         */
        REVEAL_COMPARE_RES,
    }

    /**
     * private constructor.
     */
    private QuickSorterPtoDesc() {
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
