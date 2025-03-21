package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GYW23-BP-CDPPRF protocol description. The construction comes from the following paper:
 * <p>
 * Xiaojie Guo, Kang Yang, Xiao Wang, Wenhao Zhang, Xiang Xie, Jiang Zhang, and Zheli Liu. Half-tree: Halving the cost
 * of tree expansion in COT and DPF. EUROCRYPT 2023, pp. 330-362. Cham: Springer Nature Switzerland, 2023.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
class Gyw23BpCdpprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8524915098124247871L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GYW23_BP_CDPPRF";
    /**
     * singleton mode
     */
    private static final Gyw23BpCdpprfPtoDesc INSTANCE = new Gyw23BpCdpprfPtoDesc();

    /**
     * private constructor.
     */
    private Gyw23BpCdpprfPtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends (c_1, ..., c_n)
         */
        SENDER_SEND_CORRELATION,
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
