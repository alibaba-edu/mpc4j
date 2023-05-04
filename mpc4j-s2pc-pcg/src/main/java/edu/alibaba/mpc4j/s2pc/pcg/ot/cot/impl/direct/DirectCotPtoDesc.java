package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * direct COT protocol description. This protocol directly invoke core COT.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
class DirectCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 9072724171080530799L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DIRECT_COT";
    /**
     * singleton mode
     */
    private static final DirectCotPtoDesc INSTANCE = new DirectCotPtoDesc();

    /**
     * private constructor
     */
    private DirectCotPtoDesc() {
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
