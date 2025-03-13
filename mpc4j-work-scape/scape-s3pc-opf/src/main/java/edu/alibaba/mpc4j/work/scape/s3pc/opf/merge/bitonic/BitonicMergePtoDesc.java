package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.bitonic;

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
 * @date 2025/2/21
 */
public class BitonicMergePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3650419395003624176L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "MERGE_BITONIC";

    /**
     * singleton mode
     */
    private static final BitonicMergePtoDesc INSTANCE = new BitonicMergePtoDesc();

    /**
     * private constructor
     */
    private BitonicMergePtoDesc() {
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
