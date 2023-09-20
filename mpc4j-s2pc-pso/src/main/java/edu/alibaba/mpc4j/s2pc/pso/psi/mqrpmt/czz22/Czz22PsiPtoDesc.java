package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CZZ22-PSI protocol information. The protocol comes from the following paper:
 * <p>
 * Chen, Yu, Min Zhang, Cong Zhang, and Minglang Dong. Private Set Operations from Multi-Query Reverse Private
 * Membership Test. Cryptology ePrint Archive (2022).
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Czz22PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5022494113030939769L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CZZ22_PSI";
    /**
     * singleton mode
     */
    private static final Czz22PsiPtoDesc INSTANCE = new Czz22PsiPtoDesc();

    /**
     * private constructor.
     */
    private Czz22PsiPtoDesc() {
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
