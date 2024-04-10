package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Information of aby3 shuffling protocols
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public class Aby3ShufflePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4860055054064394397L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ABY3_SHUFFLE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * 2 share -> 3 share
         */
        TWO_SHARE_INTO_THREE_SHARE,
        /**
         * send data to a specific party
         */
        COMM_WITH_SPECIFIC_PARTY,
        /**
         * msg in duplicate network
         */
        PERMUTE_MSG,
        /**
         * msg in shuffling
         */
        SHUFFLE_MSG,
        /**
         * msg in duplicate network
         */
        DUPLICATE_MSG,
    }

    /**
     * singleton mode
     */
    private static final Aby3ShufflePtoDesc INSTANCE = new Aby3ShufflePtoDesc();

    /**
     * private constructor
     */
    private Aby3ShufflePtoDesc() {
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
