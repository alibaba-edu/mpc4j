package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * DSZ15 OT-based Zl triple generation protocol description. The original scheme is described in
 * Section 3.A.5:
 * <p>
 * Daniel Demmler, Thomas Schneider, Michael Zohner: ABY - A Framework for Efficient Mixed-Protocol Secure Two-Party
 * Computation. NDSS 2015.
 * </p>
 * Note that the discussion section of DSZ15 states that we can further decrease the communication sort, as follows:
 * <p>
 * To generate an l-bit multiplication triple, P_0 and P_1 run COT_l^{2l}, where each party evaluates 6l symmetric
 * cryptographic operations and sends 2l(κ + l) bits. The communication can be further decreased by sending only the
 * l − i least significant bits in the i-th COT, since the i most significant bits are cut off by the modulo operation
 * anyway. This reduces the communication to COT_l^2 + COT_{l - 1}^2 + ... + COT_1^2, which averages to
 * COT_{(l + 1) / 2}^{2l}.
 * </p>
 * Our implementation leverages this optimization.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
class DirectZlTripleGenPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7210900674911199117L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "DIRECT_Zl_TRIPLE_GENERATION";

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
    private static final DirectZlTripleGenPtoDesc INSTANCE = new DirectZlTripleGenPtoDesc();

    /**
     * the private constructor
     */
    private DirectZlTripleGenPtoDesc() {
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
