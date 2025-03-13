package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The general semi-equal-join protocol
 * The scheme comes from the following paper:
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
public class Hzf22GeneralSemiJoinPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -16099367818860342L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GENERAL_SEMI_JOIN_HZF22";

    /**
     * singleton mode
     */
    private static final Hzf22GeneralSemiJoinPtoDesc INSTANCE = new Hzf22GeneralSemiJoinPtoDesc();

    /**
     * private constructor
     */
    private Hzf22GeneralSemiJoinPtoDesc() {
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
