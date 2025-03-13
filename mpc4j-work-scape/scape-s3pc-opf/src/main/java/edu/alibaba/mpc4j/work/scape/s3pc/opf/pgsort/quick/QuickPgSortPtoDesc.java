package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Quick Sorter for permutation generation.
 * The scheme comes from the following paper:
 *
 * <p>
 * Toshinori Araki, Jun Furukawa, et al. 2021. Secure Graph Analysis at Scale.
 * CCS 2021
 * </p>
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class QuickPgSortPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2991078162569569985L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "QUICK_PG_SORT";

    /**
     * singleton mode
     */
    private static final QuickPgSortPtoDesc INSTANCE = new QuickPgSortPtoDesc();

    /**
     * private constructor
     */
    private QuickPgSortPtoDesc() {
        // empty
    }

    public enum PtoStep {
        /**
         * send the seed for randomness generation, which decide the Pivot in sorting process
         */
        SEND_SEED,
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

