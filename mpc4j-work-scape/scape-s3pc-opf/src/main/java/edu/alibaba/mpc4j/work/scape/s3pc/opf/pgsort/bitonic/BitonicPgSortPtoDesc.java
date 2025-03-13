package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.bitonic;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Bitonic Sorter for permutation generation.
 * The scheme comes from the following paper:
 *
 * <p>
 * Kenneth E. Batcher. 1968. Sorting Networks and Their Applications. In American Federation of Information Processing
 * Societies: AFIPS, Vol. 32. Thomson Book Company, Washington D.C., 307–314.
 * </p>
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class BitonicPgSortPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -3468217850037075107L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BITONIC_PG_SORT";

    /**
     * singleton mode
     */
    private static final BitonicPgSortPtoDesc INSTANCE = new BitonicPgSortPtoDesc();

    /**
     * private constructor
     */
    private BitonicPgSortPtoDesc() {
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
