package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 naive PDSM by directly invoking PESM.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
class Cgs22NaivePdsmPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6145521838709539677L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGS22_NAIVE_PSM";
    /**
     * singleton mode
     */
    private static final Cgs22NaivePdsmPtoDesc INSTANCE = new Cgs22NaivePdsmPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22NaivePdsmPtoDesc() {
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
