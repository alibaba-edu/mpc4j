package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.silent;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Silent Z2 triple generation protocol description. ZCL23 first introduces silent OT to generate Z2 triple.
 * <p>
 * 1. S and R perform a silent R-OT. S obtains bits x0, x1 and R obtains bit a and x_a as output.<br>
 * 2. R sets u = xa; S sets b = x0 ⊕ x1 and v = x0.<br>
 * 3. R outputs (a, u) and S outputs (b, v).
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
class SilentZ2TripleGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3694804137313855423L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SILENT_Z2_TRIPLE_GENERATION";

    /**
     * private constructor.
     */
    private SilentZ2TripleGenPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final SilentZ2TripleGenPtoDesc INSTANCE = new SilentZ2TripleGenPtoDesc();

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
