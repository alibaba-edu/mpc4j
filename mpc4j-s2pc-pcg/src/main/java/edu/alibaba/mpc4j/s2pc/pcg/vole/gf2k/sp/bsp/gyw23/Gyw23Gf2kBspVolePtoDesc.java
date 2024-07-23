package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GYW23 GF2K-SSP-VOLE protocol description. The protocol comes from the following paper:
 * <p>
 * Xiaojie Guo, Kang Yang, Xiao Wang, Wenhao Zhang, Xiang Xie, Jiang Zhang, and Zheli Liu. Half-tree: Halving the cost
 * of tree expansion in COT and DPF. EUROCRYPT 2023, pp. 330-362. Cham: Springer Nature Switzerland, 2023.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/6/9
 */
class Gyw23Gf2kBspVolePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7041551589238480780L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GYW23_GF2K_BSP_VOLE";

    /**
     * private constructor
     */
    private Gyw23Gf2kBspVolePtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends d := s − β ∈ F
         */
        SENDER_SEND_DS,
        /**
         * receiver sends (c_1, ..., c_{n-1}, µ, c_n^0, c_n^1, ψ)
         */
        RECEIVER_SEND_CORRELATIONS,
    }

    /**
     * singleton mode
     */
    private static final Gyw23Gf2kBspVolePtoDesc INSTANCE = new Gyw23Gf2kBspVolePtoDesc();

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
