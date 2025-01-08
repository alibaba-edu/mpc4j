package edu.alibaba.mpc4j.s2pc.opf.osorter.bitonic;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * bitonic sorter description
 *
 * @author Feng Han
 * @date 2024/10/8
 */
public class BitonicSorterPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8718922077034199683L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BITONIC_SORTER";

    /**
     * the singleton mode
     */
    private static final BitonicSorterPtoDesc INSTANCE = new BitonicSorterPtoDesc();

    /**
     * private constructor.
     */
    private BitonicSorterPtoDesc() {
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
