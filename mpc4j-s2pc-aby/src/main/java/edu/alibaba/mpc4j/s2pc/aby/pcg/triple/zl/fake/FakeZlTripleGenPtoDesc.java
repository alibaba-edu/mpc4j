package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.fake;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * fake Zl triple generation protocol description.
 *
 * @author Weiran Liu
 * @date 2024/6/29
 */
class FakeZlTripleGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8303266241901531219L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "FAKE_Zl_TRIPLE_GENERATION";

    /**
     * private constructor.
     */
    private FakeZlTripleGenPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final FakeZlTripleGenPtoDesc INSTANCE = new FakeZlTripleGenPtoDesc();

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
