package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.silent;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * silent Zl64 triple generation protocol description.
 *
 * @author Weiran Liu
 * @date 2024/7/1
 */
class SilentZl64TripleGenPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8826067070815304992L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "SILENT_Zl64_TRIPLE_GENERATION";

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
    private static final SilentZl64TripleGenPtoDesc INSTANCE = new SilentZl64TripleGenPtoDesc();

    /**
     * the private constructor
     */
    private SilentZl64TripleGenPtoDesc() {
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
