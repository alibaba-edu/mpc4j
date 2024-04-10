package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * information of replicated 3p sharing z2 basic environment
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class RpZ2EnvPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1861188677566895507L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ABB3_MTG_ENV_Z2";

    /**
     * singleton mode
     */
    private static final RpZ2EnvPtoDesc INSTANCE = new RpZ2EnvPtoDesc();

    /**
     * private constructor
     */
    private RpZ2EnvPtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    protected enum PtoStep {
        /**
         * open the share
         */
        OPEN_SHARE,
        /**
         * compare view
         */
        COMPARE_VIEW,
        /**
         * and operation
         */
        AND_OP
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
