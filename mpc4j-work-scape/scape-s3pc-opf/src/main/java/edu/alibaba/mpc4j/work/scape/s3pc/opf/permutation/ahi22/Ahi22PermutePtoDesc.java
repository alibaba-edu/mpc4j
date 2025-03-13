package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.ahi22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Information of three-party oblivious permutation protocols
 * The scheme comes from the following paper:
 * <p>
 * Gilad Asharov, Koki Hamada, Dai Ikarashi, et al.
 * Efficient Secure Three-Party Sorting with Applications to Data Analysis and Heavy Hitters.
 * CCS 2022
 * and:
 * <p>
 * Toshinori Araki, Jun Furukawa, et al. 2021. Secure Graph Analysis at Scale.
 * CCS 2021
 * </p>
 *
 *
 * @author Feng Han
 * @date 2024/02/22
 */
public class Ahi22PermutePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -3554893889855319203L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PERMUTE_AHI+22";

    /**
     * singleton mode
     */
    private static final Ahi22PermutePtoDesc INSTANCE = new Ahi22PermutePtoDesc();

    /**
     * private constructor
     */
    private Ahi22PermutePtoDesc() {
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
