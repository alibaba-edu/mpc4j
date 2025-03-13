package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The order-by protocol, where the payload is permuted after sorting the key
 * The scheme comes from the following paper:
 *
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li.
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class Hzf22OrderByPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4151054262161743918L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ORDER_BY_HZF22";

    /**
     * singleton mode
     */
    private static final Hzf22OrderByPtoDesc INSTANCE = new Hzf22OrderByPtoDesc();

    /**
     * private constructor
     */
    private Hzf22OrderByPtoDesc() {
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
