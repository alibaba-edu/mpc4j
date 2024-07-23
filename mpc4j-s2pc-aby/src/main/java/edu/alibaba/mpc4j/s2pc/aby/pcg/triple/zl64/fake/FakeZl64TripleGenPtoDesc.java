package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.fake;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * fake Zl64 triple generation protocol description.
 *
 * @author Weiran Liu
 * @date 2024/6/30
 */
class FakeZl64TripleGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8591767832844614349L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "FAKE_Zl64_TRIPLE_GENERATION";

    /**
     * private constructor.
     */
    private FakeZl64TripleGenPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final FakeZl64TripleGenPtoDesc INSTANCE = new FakeZl64TripleGenPtoDesc();

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
