package edu.alibaba.mpc4j.s2pc.pso.psu.czz24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


/**
 * CZZ24-cwOPRF-PSU protocol description. The protocol comes from the following paper:
 * <p>
 * Yu Chen, Min Zhang, Cong Zhang, Minglang Dong, and Weiran Liu. Private set operations from multi-query reverse
 * private membership test. PKC 2024, pp. 387-416. Cham: Springer Nature Switzerland, 2024.
 * </p>
 *
 * @author Yufei Wang
 * @date 2023/8/1
 */
class Czz24CwOprfPsuPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1015842095203839577L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CZZ24_cwOPRF_PSU";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends encrypted elements
         */
        SERVER_SEND_ENC_ELEMENTS
    }

    /**
     * singleton mode
     */
    private static final Czz24CwOprfPsuPtoDesc INSTANCE = new Czz24CwOprfPsuPtoDesc();

    /**
     * private constructor
     */
    private Czz24CwOprfPsuPtoDesc() {
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
