package edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * sq-OPRF-based PSI protocol description.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
class SqOprfPsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1222254604394141319L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SQ_OPRF_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends PRF filter
         */
        SERVER_SEND_PRF_FILTER,
    }

    /**
     * singleton mode
     */
    private static final SqOprfPsiPtoDesc INSTANCE = new SqOprfPsiPtoDesc();

    /**
     * private constructor.
     */
    private SqOprfPsiPtoDesc() {
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
