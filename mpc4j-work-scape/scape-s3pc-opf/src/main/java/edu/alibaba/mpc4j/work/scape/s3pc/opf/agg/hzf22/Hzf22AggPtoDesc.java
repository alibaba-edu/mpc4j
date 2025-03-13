package edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Protocols for the aggregate function in the following paper:
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 *
 * @author Feng Han
 * @date 2025/2/26
 */
public class Hzf22AggPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6280723996483218047L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "AGG_HZF22";

    /**
     * singleton mode
     */
    private static final Hzf22AggPtoDesc INSTANCE = new Hzf22AggPtoDesc();

    /**
     * private constructor
     */
    private Hzf22AggPtoDesc() {
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
