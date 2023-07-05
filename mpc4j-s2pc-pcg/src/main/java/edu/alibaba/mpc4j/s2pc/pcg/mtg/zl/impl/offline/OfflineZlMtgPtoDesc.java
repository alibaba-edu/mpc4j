package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.offline;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * offline Zl multiplication triple generator protocol description.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
class OfflineZlMtgPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6076274914721430171L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "OFFLINE_ZL_MTG";
    /**
     * singleton mode
     */
    private static final OfflineZlMtgPtoDesc INSTANCE = new OfflineZlMtgPtoDesc();

    /**
     * private constructor.
     */
    private OfflineZlMtgPtoDesc() {
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
