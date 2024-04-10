package edu.alibaba.mpc4j.s2pc.upso.okvr.kw;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Labeled PSI-based OKVR protocol description.
 *
 * @author Weiran Liu
 * @date 2024/2/3
 */
class KwOkvrPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 413119364415615857L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "KW_OKVR";
    /**
     * the singleton mode
     */
    private static final KwOkvrPtoDesc INSTANCE = new KwOkvrPtoDesc();

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the sender sends OKVS keys
         */
        SENDER_SEND_OKVS_KEYS,
        /**
         * the sender sends dense OKVS
         */
        SENDER_SEND_DENSE_OKVS,
    }

    /**
     * private constructor.
     */
    private KwOkvrPtoDesc() {
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
