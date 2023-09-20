package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
class Gmr21PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2423946578643819624L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GMR21_PSI";
    /**
     * singleton mode
     */
    private static final Gmr21PsiPtoDesc INSTANCE = new Gmr21PsiPtoDesc();

    /**
     * private constructor.
     */
    private Gmr21PsiPtoDesc() {
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
