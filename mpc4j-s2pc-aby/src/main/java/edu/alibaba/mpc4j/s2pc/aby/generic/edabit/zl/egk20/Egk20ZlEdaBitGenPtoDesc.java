package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.egk20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * EGK+20 semi-honest Zl edaBit generation protocol description. The protocol is described in Figure 14 of the following
 * paper:
 * <p>
 * Escudero, Daniel, Satrajit Ghosh, Marcel Keller, Rahul Rachuri, and Peter Scholl. Improved primitives for MPC over
 * mixed arithmetic-binary circuits. CRYPTO 2020, Part II 40, pp. 823-852. Springer International Publishing, 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
class Egk20ZlEdaBitGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4906166849777230026L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "EGK20_WITH_MAC";
    /**
     * singleton mode
     */
    private static final Egk20ZlEdaBitGenPtoDesc INSTANCE = new Egk20ZlEdaBitGenPtoDesc();

    /**
     * private constructor
     */
    private Egk20ZlEdaBitGenPtoDesc() {
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
