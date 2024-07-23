package edu.alibaba.mpc4j.work.payable.pir.baseline;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Baseline payable PIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class BaselinePayablePirPtoDesc implements PtoDesc {

    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1373189135199014622L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BASELINE_PAYABLE_PIR";

    /**
     * the singleton mode
     */
    private static final BaselinePayablePirPtoDesc INSTANCE = new BaselinePayablePirPtoDesc();

    /**
     * private constructor.
     */
    private BaselinePayablePirPtoDesc() {
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
