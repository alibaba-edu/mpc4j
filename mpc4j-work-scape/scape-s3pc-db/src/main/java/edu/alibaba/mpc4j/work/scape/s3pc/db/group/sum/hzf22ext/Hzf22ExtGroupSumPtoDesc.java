package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The group sum protocol
 * The scheme comes from the extension of the following paper:
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li.
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 * the idea is almost the same as the paper:
 * <p>
 * Nuttapong Attrapadung, et al.
 * Secure Parallel Computation on Privately Partitioned Data and Applications
 * CCS 2022
 * </p>
 *
 * @author Feng Han
 * @date 2025/3/3
 */
public class Hzf22ExtGroupSumPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4973113976698805216L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GROUP_SUM_HZF22_EXT";

    /**
     * singleton mode
     */
    private static final Hzf22ExtGroupSumPtoDesc INSTANCE = new Hzf22ExtGroupSumPtoDesc();

    /**
     * private constructor
     */
    private Hzf22ExtGroupSumPtoDesc() {
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
