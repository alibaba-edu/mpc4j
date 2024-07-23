package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.cot;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * cache 1-out-of-n (with n = 2^l) OT protocol description.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
class CotLnotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1351248120450099866L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "COT_LNOT";
    /**
     * singleton mode
     */
    private static final CotLnotPtoDesc INSTANCE = new CotLnotPtoDesc();

    /**
     * private constructor.
     */
    private CotLnotPtoDesc() {
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
