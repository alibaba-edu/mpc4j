package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * cache COT protocol description.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
class CacheCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6300464192699515574L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CACHE_COT";
    /**
     * singleton mode
     */
    private static final CacheCotPtoDesc INSTANCE = new CacheCotPtoDesc();

    /**
     * private constructor.
     */
    private CacheCotPtoDesc() {
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
