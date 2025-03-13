package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The pkpk semi-join protocol
 * The scheme comes from the following paper:
 *
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li.
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public class Hzf22PkPkSemiJoinPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -3922810620623552809L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PK_PK_SEMI_JOIN_HZF22";

    /**
     * singleton mode
     */
    private static final Hzf22PkPkSemiJoinPtoDesc INSTANCE = new Hzf22PkPkSemiJoinPtoDesc();

    /**
     * private constructor
     */
    private Hzf22PkPkSemiJoinPtoDesc() {
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
