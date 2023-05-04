package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * direct 1-out-of-n (with n = 2^l) OT protocol description. This protocol directly invoke 1-out-of-2^l COT.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
class DirectLnotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2400323698523644978L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DIRECT_LNOT";
    /**
     * singleton mode
     */
    private static final DirectLnotPtoDesc INSTANCE = new DirectLnotPtoDesc();

    /**
     * private constructor
     */
    private DirectLnotPtoDesc() {
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
