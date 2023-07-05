package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * EGK+20 semi-honest Zl (no MAC) daBit generation protocol description. The protocol is described in Figure 14 of the
 * following paper:
 * <p>
 * Escudero, Daniel, Satrajit Ghosh, Marcel Keller, Rahul Rachuri, and Peter Scholl. Improved primitives for MPC over
 * mixed arithmetic-binary circuits. CRYPTO 2020, Part II 40, pp. 823-852. Springer International Publishing, 2020.
 * </p>
 * We note that the difference between with MAC and without MAC is that, if we require MAC, we need to explicitly share
 * the Z2 vector (so that we can generate the MAC); otherwise, we can directly treat the least significant bit as the
 * shares of Z2.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
class Egk20NoMacZlDaBitGenPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 610953581318651334L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "EGK20_NO_MAC";
    /**
     * singleton mode
     */
    private static final Egk20NoMacZlDaBitGenPtoDesc INSTANCE = new Egk20NoMacZlDaBitGenPtoDesc();

    /**
     * private constructor
     */
    private Egk20NoMacZlDaBitGenPtoDesc() {
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
