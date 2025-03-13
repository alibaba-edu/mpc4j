package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The scheme comes from the protocol P_{p2Per} in the following paper:
 *
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li.
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 *
 * @author Feng Han
 * @date 2025/2/26
 */
public class Hzf22FillPermutationPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7357837217243436341L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SOPRP_BASED_HZF22";

    /**
     * singleton mode
     */
    private static final Hzf22FillPermutationPtoDesc INSTANCE = new Hzf22FillPermutationPtoDesc();

    /**
     * private constructor
     */
    private Hzf22FillPermutationPtoDesc() {
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
