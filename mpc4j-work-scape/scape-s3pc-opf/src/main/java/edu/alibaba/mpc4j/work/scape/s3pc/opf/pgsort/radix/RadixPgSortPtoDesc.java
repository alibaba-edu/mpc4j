package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Information of three-party permutation-generation sort protocols.
 * The scheme comes from the following paper:
 *
 * <p>
 * Gilad Asharov, Koki Hamada, Dai Ikarashi, et al.
 * Efficient Secure Three-Party Sorting with Applications to Data Analysis and Heavy Hitters.
 * CCS 2022
 * </p>
 *
 * @author Feng Han
 * @date 2024/02/27
 */
public class RadixPgSortPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 935577603854295003L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RADIX_PG_SORT";

    /**
     * singleton mode
     */
    private static final RadixPgSortPtoDesc INSTANCE = new RadixPgSortPtoDesc();

    /**
     * private constructor
     */
    private RadixPgSortPtoDesc() {
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

