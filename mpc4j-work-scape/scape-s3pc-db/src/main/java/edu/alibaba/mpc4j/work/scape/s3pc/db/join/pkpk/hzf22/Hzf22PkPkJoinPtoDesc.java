package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The pkpk join protocol
 * The scheme comes from the following paper:
 *
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li.
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 *
 * @author Feng Han
 * @date 2025/02/21
 */
public class Hzf22PkPkJoinPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4178688397588609617L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PK_PK_JOIN_HZF22";

    /**
     * singleton mode
     */
    private static final Hzf22PkPkJoinPtoDesc INSTANCE = new Hzf22PkPkJoinPtoDesc();

    /**
     * private constructor
     */
    private Hzf22PkPkJoinPtoDesc() {
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
