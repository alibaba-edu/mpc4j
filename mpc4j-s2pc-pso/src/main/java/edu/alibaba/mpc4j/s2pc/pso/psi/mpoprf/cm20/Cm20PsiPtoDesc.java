package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CM20-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Chase M, Miao P. Private Set Intersection in the Internet Setting from Lightweight Oblivious PRF. CRYPTO 2020.
 * pp. 34-63.
 * <p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/10
 */
class Cm20PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8849499987257147091L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CM20_PSI";
    /**
     * singleton mode
     */
    private static final Cm20PsiPtoDesc INSTANCE = new Cm20PsiPtoDesc();

    /**
     * private constructor.
     */
    private Cm20PsiPtoDesc() {
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
