package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rs21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RS21-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: fast OPRF and circuit-PSI from vector-OLE. EUROCRYPT 2021,
 * pp. 901-930. Cham: Springer International Publishing, 2021.
 * </p>
 * @author Weiran Liu
 * @date 2023/9/18
 */
class Rs21PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7791198912734009619L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RS21_PSI";
    /**
     * singleton mode
     */
    private static final Rs21PsiPtoDesc INSTANCE = new Rs21PsiPtoDesc();

    /**
     * private constructor.
     */
    private Rs21PsiPtoDesc() {
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
