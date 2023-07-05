package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * naive private equality test protocol description. This protocol does bit-wise AND / OR for PEQT.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
class NaivePeqtPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 792694580069278595L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "NAIVE_PEQT";
    /**
     * singleton mode
     */
    private static final NaivePeqtPtoDesc INSTANCE = new NaivePeqtPtoDesc();

    /**
     * private constructor.
     */
    private NaivePeqtPtoDesc() {
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
