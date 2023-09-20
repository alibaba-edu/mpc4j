package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * mq-RPMT-based PSI protocol description.
 *
 * @author Weiran Liu
 * @date 2023/9/9
 */
class MqRpmtPsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4419959567898004727L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "MQ_RPMT_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends encrypted elements
         */
        SERVER_SEND_CIPHER,
    }

    /**
     * singleton mode
     */
    private static final MqRpmtPsiPtoDesc INSTANCE = new MqRpmtPsiPtoDesc();

    /**
     * private constructor.
     */
    private MqRpmtPsiPtoDesc() {
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
