package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * direct Zl64 triple generation protocol description.
 *
 * @author Weiran Liu
 * @date 2024/6/30
 */
class DirectZl64TripleGenPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2689854420146269956L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "DIRECT_Zl64_TRIPLE_GENERATION";

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
    private static final DirectZl64TripleGenPtoDesc INSTANCE = new DirectZl64TripleGenPtoDesc();

    /**
     * the private constructor
     */
    private DirectZl64TripleGenPtoDesc() {
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
