package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * YWL20-SP-DPPRF description. The scheme comes from:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 * We note that the following paper:
 * <p>
 * Guo, Xiaojie, Kang Yang, Xiao Wang, Wenhao Zhang, Xiang Xie, Jiang Zhang, and Zheli Liu. Half-Tree: Halving the Cost
 * of Tree Expansion in COT and DPF. EUROCRYPT 2023.
 * </p>
 * implicitly states that one can have a random-α scheme by treating choice bits of
 * pre-computed COTs to be !α, saving one round. However, this optimization requires that n = 2^h. The reason is that
 * choice bits of pre-computed COTs are random, combing these to get α means α ∈ {0, 1}^h. If n != 2^h, α can possibly
 * be out of range in [0, n). Therefore, we do not provide random-α interfaces.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class Ywl20SpDpprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7440499850777158783L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "YWL20_SP_DPPRF";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * i ∈ {1,...,h}, S sends M_0^i = K_0^i ⊕ H(q_i ⊕ b_i ∆, i || l), M_1^i = K_1^i ⊕ H(q_i ⊕ \not b_i ∆, i || l)
         */
        SENDER_SEND_MESSAGE,
    }

    /**
     * singleton mode
     */
    private static final Ywl20SpDpprfPtoDesc INSTANCE = new Ywl20SpDpprfPtoDesc();

    /**
     * private constructor
     */
    private Ywl20SpDpprfPtoDesc() {
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
