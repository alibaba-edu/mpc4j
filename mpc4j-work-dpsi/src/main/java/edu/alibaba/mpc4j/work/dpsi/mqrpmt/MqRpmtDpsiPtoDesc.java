package edu.alibaba.mpc4j.work.dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * DPSI based on mqRPMT protocol description.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/9/18
 */
class MqRpmtDpsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 731131751523952969L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "mqRPMT_DP_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * client sends element size
         */
        CLIENT_SEND_ELEMENTS_SIZE,
        /**
         * server sends randomized vector
         */
        SERVER_SEND_RANDOMIZED_VECTOR
    }

    /**
     * private constructor
     */
    private MqRpmtDpsiPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final MqRpmtDpsiPtoDesc INSTANCE = new MqRpmtDpsiPtoDesc();

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
