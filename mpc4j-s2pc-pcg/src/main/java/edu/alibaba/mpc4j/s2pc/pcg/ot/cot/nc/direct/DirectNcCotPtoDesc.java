package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * direct no-choice COT protocol description. This protocol directly invoke OT extensions.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
class DirectNcCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6192153866874601909L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DIRECT_NC_COT";

    /**
     * singleton mode
     */
    private static final DirectNcCotPtoDesc INSTANCE = new DirectNcCotPtoDesc();

    /**
     * private constructor
     */
    private DirectNcCotPtoDesc() {
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
