package edu.alibaba.mpc4j.work.payable.psi.baseline;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Baseline payable PSI protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public class BaselinePayablePsiPtoDesc implements PtoDesc {

    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7431946488291404579L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BASELINE_PAYABLE_PSI";

    /**
     * the singleton mode
     */
    private static final BaselinePayablePsiPtoDesc INSTANCE = new BaselinePayablePsiPtoDesc();

    /**
     * private constructor.
     */
    private BaselinePayablePsiPtoDesc() {
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