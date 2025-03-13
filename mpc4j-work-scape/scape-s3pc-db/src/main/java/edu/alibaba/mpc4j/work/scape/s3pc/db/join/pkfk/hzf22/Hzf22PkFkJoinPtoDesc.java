package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.hzf22;

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
 * @date 2024/03/21
 */
public class Hzf22PkFkJoinPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5821082500618331315L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PK_FK_JOIN_HZF22";

    /**
     * singleton mode
     */
    private static final Hzf22PkFkJoinPtoDesc INSTANCE = new Hzf22PkFkJoinPtoDesc();

    /**
     * private constructor
     */
    private Hzf22PkFkJoinPtoDesc() {
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
