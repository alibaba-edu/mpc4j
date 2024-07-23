package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.prrs24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PRRS24 OPRF Random OSN protocol description. The construction comes from the following paper:
 * <p>
 * Stanislav Peceny, Srinivasan Raghuraman, Peter Rindal, Harshal Shah. Efficient Permutation Correlations and Batched
 * Random Access for Two-Party Computation. ePrint archive, 2024.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/6/7
 */
class Prrs24OprfRosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3351734995446918746L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PRRS24_OPRF_ROSN";

    enum PtoStep {
        /**
         * receiver sends t
         */
        RECEIVER_SEND_T,
    }

    /**
     * private constructor.
     */
    private Prrs24OprfRosnPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Prrs24OprfRosnPtoDesc INSTANCE = new Prrs24OprfRosnPtoDesc();

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
