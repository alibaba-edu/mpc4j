package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.opt;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The somewhat-optimal sorting strategy for different input data domain
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class OptPgSortPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1395277241427100819L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "OPT_PG_SORT";

    /**
     * singleton mode
     */
    private static final OptPgSortPtoDesc INSTANCE = new OptPgSortPtoDesc();

    /**
     * private constructor
     */
    private OptPgSortPtoDesc() {
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

