package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * direct no-choice 1-out-of-n (with n = 2^l) OT protocol description. This protocol directly invokes 1-out-of-2^l COT.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
class DirectNcLnotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6416466057413996131L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DIRECT_NC_LNOT";

    /**
     * singleton mode
     */
    private static final DirectNcLnotPtoDesc INSTANCE = new DirectNcLnotPtoDesc();

    /**
     * private constructor
     */
    private DirectNcLnotPtoDesc() {
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
