package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The sorting strategy in HZF22 for different input data domain
 * The scheme used in the following paper:
 *
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li.
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class Hzf22PgSortPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2801036436826384454L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "HZF22_PG_SORT";

    /**
     * singleton mode
     */
    private static final Hzf22PgSortPtoDesc INSTANCE = new Hzf22PgSortPtoDesc();

    /**
     * private constructor
     */
    private Hzf22PgSortPtoDesc() {
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

