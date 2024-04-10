package edu.alibaba.mpc4j.s3pc.abb3.context.cr;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Description of Correlated randomness provider for 3PC
 *
 * @author Feng Han
 * @date 2023/12/25
 */
public class S3pcCrProviderPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1293344148581001143L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ABB3_CR_PROVIDER";

    /**
     * singleton mode
     */
    private static final S3pcCrProviderPtoDesc INSTANCE = new S3pcCrProviderPtoDesc();

    /**
     * private constructor
     */
    private S3pcCrProviderPtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    protected enum PtoStep {
        /**
         * communicate the parallel info
         */
        COMM_PARALLEL,
        /**
         * share the prp key
         */
        KEY_SHARE,
    }

    /**
     * the type of correlated randomness
     */
    public enum RandomType{
        /**
         * 3p share
         */
        SHARE,
        /**
         * share with left party
         */
        WITH_LEFT,
        /**
         * share with right party
         */
        WITH_RIGHT,
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
