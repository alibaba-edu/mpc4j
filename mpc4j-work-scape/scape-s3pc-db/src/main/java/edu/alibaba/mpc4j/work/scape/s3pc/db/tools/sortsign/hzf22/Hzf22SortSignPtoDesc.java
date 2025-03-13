package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The sort and equal sign generation protocol for general (semi-)join
 * The scheme comes from the following paper:
 *
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li.
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 *
 * @author Feng Han
 * @date 2024/03/08
 */
public class Hzf22SortSignPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -5870913412889130999L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "HZF22_SORT_SIGN";

    /**
     * singleton mode
     */
    private static final Hzf22SortSignPtoDesc INSTANCE = new Hzf22SortSignPtoDesc();

    /**
     * private constructor
     */
    private Hzf22SortSignPtoDesc() {
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
