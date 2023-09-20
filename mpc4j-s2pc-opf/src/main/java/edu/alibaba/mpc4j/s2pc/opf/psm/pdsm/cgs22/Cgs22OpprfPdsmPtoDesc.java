package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 OPPRF-based PDSM protocol description. The protocol is described in Fig. 7 of the following paper:
 * <p>
 * Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI With Linear Complexity via Relaxed Batch OPPRF.
 * PETS 2022, pp. 353-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
class Cgs22OpprfPdsmPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7307608506079798534L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGS22_OPPRF_PDSM";

    /**
     * singleton mode
     */
    private static final Cgs22OpprfPdsmPtoDesc INSTANCE = new Cgs22OpprfPdsmPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22OpprfPdsmPtoDesc() {
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
