package edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * FIPR05 multi-query OPRF protocol description. Freedman, Ishai, Pinkas and Reingold first defines OPRF and provide a
 * construction in the following paper:
 * <p>
 * Freedman, Michael J., Yuval Ishai, Benny Pinkas, and Omer Reingold. Keyword Search and Oblivious Pseudorandom
 * Functions. TCC 2005, pp. 303-324. 2005.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
class Fipr05MpOprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 14562759458471042L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "FIPS05_MPOPRF";
    /**
     * singleton mode
     */
    private static final Fipr05MpOprfPtoDesc INSTANCE = new Fipr05MpOprfPtoDesc();

    /**
     * private constructor
     */
    private Fipr05MpOprfPtoDesc() {
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
