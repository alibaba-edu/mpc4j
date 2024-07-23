package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.silent;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * silent Zl triple generation protocol description.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
class SilentZlTripleGenPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1320644737737538476L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "SILENT_Zl_TRIPLE_GENERATION";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the receiver sends the correlation.
         */
        RECEIVER_SEND_CORRELATION,
        /**
         * the sender sends the correlation.
         */
        SENDER_SEND_CORRELATION,
    }

    /**
     * the singleton mode
     */
    private static final SilentZlTripleGenPtoDesc INSTANCE = new SilentZlTripleGenPtoDesc();

    /**
     * the private constructor
     */
    private SilentZlTripleGenPtoDesc() {
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
