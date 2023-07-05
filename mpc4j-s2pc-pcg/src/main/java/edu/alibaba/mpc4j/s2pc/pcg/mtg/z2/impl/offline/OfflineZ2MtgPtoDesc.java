package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * offline Z2 multiplication triple generator protocol description.
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
class OfflineZ2MtgPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4637156677746717932L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "OFFLINE_Z2_MTG";
    /**
     * singleton mode
     */
    private static final OfflineZ2MtgPtoDesc INSTANCE = new OfflineZ2MtgPtoDesc();

    /**
     * private constructor.
     */
    private OfflineZ2MtgPtoDesc() {
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
