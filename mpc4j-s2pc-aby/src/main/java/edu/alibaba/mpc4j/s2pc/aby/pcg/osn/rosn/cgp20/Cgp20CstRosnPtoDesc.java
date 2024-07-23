package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.cgp20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGP20 CST Random OSN protocol description. The construction comes from the following paper:
 * <p>
 * Chase, Melissa, Esha Ghosh, and Oxana Poburinnaya. Secret-shared shuffle. ASIACRYPT 2020, pp. 342-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
class Cgp20CstRosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4688362787745286224L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGP20_CST_ROSN";

    /**
     * private constructor.
     */
    private Cgp20CstRosnPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Cgp20CstRosnPtoDesc INSTANCE = new Cgp20CstRosnPtoDesc();

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
