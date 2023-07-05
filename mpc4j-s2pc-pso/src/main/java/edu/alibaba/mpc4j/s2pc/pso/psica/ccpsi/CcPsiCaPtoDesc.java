package edu.alibaba.mpc4j.s2pc.pso.psica.ccpsi;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * client-payload circuit-PSI Cardinality.
 *
 * @author Qixian Zhou
 * @date 2023/4/24
 */
class CcPsiCaPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6638744674193559661L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CIRCUIT_PSIC";
    /**
     * singleton mode
     */
    private static final CcPsiCaPtoDesc INSTANCE = new CcPsiCaPtoDesc();

    /**
     * private constructor
     */
    private CcPsiCaPtoDesc() {
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
