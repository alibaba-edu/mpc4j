package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Information of three-party oblivious traversal protocols.
 * The scheme comes from the following paper:
 *
 * <p>
 * Feng Han; Lan Zhang; Hanwen Feng; Weiran Liu; Xiangyang Li
 * Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security
 * ICDE 2022
 * </p>
 *
 * @author Feng Han
 * @date 2024/02/23
 */
public class Hzf22TraversalPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -2291761923813621745L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "TRAVERSAL_SCAPE";

    /**
     * singleton mode
     */
    private static final Hzf22TraversalPtoDesc INSTANCE = new Hzf22TraversalPtoDesc();

    /**
     * private constructor
     */
    private Hzf22TraversalPtoDesc() {
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
