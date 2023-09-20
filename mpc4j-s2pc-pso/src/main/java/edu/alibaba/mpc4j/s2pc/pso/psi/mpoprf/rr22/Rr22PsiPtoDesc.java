package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RR22-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Raghuraman, Srinivasan, and Peter Rindal. Blazing fast PSI from improved OKVS and subfield VOLE. CCS 2022,
 * pp. 2505-2517. 2022.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
class Rr22PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4781360283171410663L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RR22_PSI";
    /**
     * singleton mode
     */
    private static final Rr22PsiPtoDesc INSTANCE = new Rr22PsiPtoDesc();

    /**
     * private constructor.
     */
    private Rr22PsiPtoDesc() {
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
