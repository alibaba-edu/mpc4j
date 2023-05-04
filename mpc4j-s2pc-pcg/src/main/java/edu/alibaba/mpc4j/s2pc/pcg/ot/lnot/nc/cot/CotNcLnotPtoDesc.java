package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.cot;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * COT no-choice 1-out-of-n (with n = 2^l) OT protocol description. This protocol invokes silent COT.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
class CotNcLnotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1833438776853439791L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SILENT_NC_LNOT";

    /**
     * singleton mode
     */
    private static final CotNcLnotPtoDesc INSTANCE = new CotNcLnotPtoDesc();

    /**
     * private constructor
     */
    private CotNcLnotPtoDesc() {
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
