package edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The group max/min protocol
 * The scheme comes from the following paper:
 *
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li.
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class Hzf22GroupExtremePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1088463526200895249L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GROUP_EXTREME_HZF22";

    /**
     * singleton mode
     */
    private static final Hzf22GroupExtremePtoDesc INSTANCE = new Hzf22GroupExtremePtoDesc();

    /**
     * private constructor
     */
    private Hzf22GroupExtremePtoDesc() {
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
