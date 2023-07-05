package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * cache Zl multiplication triple generator protocol description.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
class CacheZlMtgPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1845063599914447658L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CACHE_ZL_MTG";
    /**
     * singleton mode
     */
    private static final CacheZlMtgPtoDesc INSTANCE = new CacheZlMtgPtoDesc();

    /**
     * private constructor
     */
    private CacheZlMtgPtoDesc() {
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
