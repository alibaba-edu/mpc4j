package edu.alibaba.mpc4j.s2pc.pso.psica.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21-PSICA protocol descrption. The protocol comes from the following paper:
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Weiran Liu, Liqiang Peng
 * @date 2022/02/15
 */
class Gmr21PsiCaPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5775416547452546358L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GMR21_PSI_CA";
    /**
     * singleton mode
     */
    private static final Gmr21PsiCaPtoDesc INSTANCE = new Gmr21PsiCaPtoDesc();

    /**
     * private constructor
     */
    private Gmr21PsiCaPtoDesc() {
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
