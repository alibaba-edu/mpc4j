package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LLL24 CST Random OSEN protocol description.
 *
 * @author Weiran Liu
 * @date 2024/5/9
 */
class Lll24CstRosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6201761208790764365L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLL24_CST_ROSN";

    /**
     * private constructor.
     */
    private Lll24CstRosnPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Lll24CstRosnPtoDesc INSTANCE = new Lll24CstRosnPtoDesc();

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
