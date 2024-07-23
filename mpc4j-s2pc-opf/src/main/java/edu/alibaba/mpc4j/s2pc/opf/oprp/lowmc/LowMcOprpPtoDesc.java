package edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LowMc-OPRP protocol description.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
class LowMcOprpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int)2566896299582292732L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LOWMC_OPRP";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends shared key
         */
        SERVER_SEND_SHARE_KEY,
        /**
         * client sends shared message
         */
        CLIENT_SEND_SHARE_MESSAGE,
        /**
         * the sender sends e0 and f0
         */
        SENDER_SEND_E0_F0,
        /**
         * the receiver sends e1 and f1
         */
        RECEIVER_SEND_E1_F1,
    }

    /**
     * singleton mode
     */
    private static final LowMcOprpPtoDesc INSTANCE = new LowMcOprpPtoDesc();

    /**
     * private constructor.
     */
    private LowMcOprpPtoDesc() {
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
