package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * information of replicated 3p sharing zl64 basic environment
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class RpLongEnvPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -6766244021216932814L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ABB3_MTG_ENV_Zl64";

    /**
     * singleton mode
     */
    private static final RpLongEnvPtoDesc INSTANCE = new RpLongEnvPtoDesc();

    /**
     * private constructor
     */
    private RpLongEnvPtoDesc() {
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
        MUL_OP
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
