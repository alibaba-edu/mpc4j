package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * mp-OPRF-based PSI protocol description.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
class MpOprfPsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8864409522249251355L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "MP_OPRF_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends PRFs
         */
        SERVER_SEND_PRFS,
    }
    
    /**
     * singleton mode
     */
    private static final MpOprfPsiPtoDesc INSTANCE = new MpOprfPsiPtoDesc();

    /**
     * private constructor.
     */
    private MpOprfPsiPtoDesc() {
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
