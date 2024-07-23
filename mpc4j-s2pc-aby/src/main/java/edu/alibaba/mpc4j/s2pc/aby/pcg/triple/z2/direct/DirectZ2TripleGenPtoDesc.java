package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Direct triple generation protocol description. The protocol comes from the following paper:
 * <p>
 * Asharov G, Lindell Y, Schneider T, et al. More efficient oblivious transfer and extensions for faster secure
 * computation. CCS 2013, ACM, 2013, pp. 535-548.
 * </p>
 * The details are shown in Section 5.1:
 * <p>
 * In order to generate a multiplication triple, we first introduce the f^{a}b functionality that is implemented in
 * Algorithm 1 using R-OT. In the f^{ab} functionality, the parties hold no input and receive random bits
 * ((a, u), (b, v)), under the constraint that a ⊙ b = u ⊕ v. Now, note that for a multiplication triple c0 ⊕ c1 =
 * (a0 ⊕ a1) ⊙ (b0 ⊕ b1) = (a0 ⊙ b0) ⊕ (a0 ⊙ b1) ⊕ (a1 ⊙ b0) ⊕ (a1 ⊙ b1). The parties can generate a multiplication
 * triple by invoking the f^{ab} functionality twice: in the first invocation P0 acts as R to obtain (a0, u0) and P1
 * acts as S to obtain (b1, v1) with a0 ⊙ b1 = u0 ⊕ v1; in the second invocation P1 acts as R to obtain (a1, u1) and P0
 * acts as S to obtain (b0, v0) with a1 ⊙ b0 = u1 ⊕ v0. Finally, each Pi sets ci = (ai ⊙ bi) ⊕ ui ⊕ vi. For correctness,
 * observe that c0 ⊕ c1 = ((a0 ⊙ b0) ⊕ u0 ⊕ v0) ⊕ ((a1 ⊙ b1) ⊕ u1 ⊕ v1) = (a0 ⊙ b0) ⊕ (u0 ⊕ v1) ⊕ (u1 ⊕ v0) ⊕ (a1 ⊙ b1)
 * = (a0 ⊙ b0) ⊕ (a0 ⊙ b1) ⊕ (a1 ⊙ b0) ⊕ (a1 ⊙ b1) = (a0 ⊕ a1) ⊙ (b0 ⊕ b1), as required.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
class DirectZ2TripleGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 158451238291507611L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DIRECT_Z2_TRIPLE_GENERATION";

    /**
     * private constructor.
     */
    private DirectZ2TripleGenPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final DirectZ2TripleGenPtoDesc INSTANCE = new DirectZ2TripleGenPtoDesc();

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
