package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * cache Z2 multiplication triple generator protocol description.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
class CacheZ2MtgPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2352412063583480191L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CACHE_Z2_MTG";
    /**
     * singleton mode
     */
    private static final CacheZ2MtgPtoDesc INSTANCE = new CacheZ2MtgPtoDesc();

    /**
     * private constructor
     */
    private CacheZ2MtgPtoDesc() {
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
