package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.rrt23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RRT23-NC-COT protocol description. The protocol comes from the following paper:
 * <p>
 * Srinivasan Raghuraman, Peter Rindal, and Titouan Tanguy. Expand-convolute codes for pseudorandom correlation
 * generators from LPN. CRYPTO 2023, pp. 602-632. Cham: Springer Nature Switzerland, 2023.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
class Rrt23NcCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8294485379533343112L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RRT23_NC_COT";

    /**
     * private constructor.
     */
    private Rrt23NcCotPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Rrt23NcCotPtoDesc INSTANCE = new Rrt23NcCotPtoDesc();

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

    /**
     * min log(n)
     */
    static final int MIN_LOG_N = 12;
    /**
     * max log(n)
     */
    static final int MAX_LOG_N = 22;
}
