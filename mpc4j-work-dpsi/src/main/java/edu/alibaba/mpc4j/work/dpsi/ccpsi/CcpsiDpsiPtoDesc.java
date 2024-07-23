package edu.alibaba.mpc4j.work.dpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * DPSI based on client-payload circuit PSI protocol description.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/8/15
 */
class CcpsiDpsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2761310131839337505L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CCPSI_DP_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sever sends randomized vector.
         */
        SERVER_SEND_RANDOMIZED_VECTOR,
    }

    /**
     * private constructor
     */
    private CcpsiDpsiPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final CcpsiDpsiPtoDesc INSTANCE = new CcpsiDpsiPtoDesc();

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