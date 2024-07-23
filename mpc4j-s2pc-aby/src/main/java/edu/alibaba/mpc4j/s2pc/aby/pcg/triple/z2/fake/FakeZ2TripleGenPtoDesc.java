package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * fake Z2 triple generation protocol description.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
class FakeZ2TripleGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3666552014363581082L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "FAKE_Z2_TRIPLE_GENERATION";

    /**
     * private constructor.
     */
    private FakeZ2TripleGenPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final FakeZ2TripleGenPtoDesc INSTANCE = new FakeZ2TripleGenPtoDesc();

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
