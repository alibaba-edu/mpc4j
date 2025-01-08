package edu.alibaba.mpc4j.s2pc.opf.shuffle.cgp20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGP20 shuffle protocol description. The construction comes from the following paper:
 * <p>
 * Chase, Melissa, Esha Ghosh, and Oxana Poburinnaya. Secret-shared shuffle. ASIACRYPT 2020, pp. 342-372.
 * </p>
 *
 * @author Feng Han
 * @date 2024/9/26
 */
public class Cgp20ShufflePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1384426525792363379L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGP20_SHUFFLE";

    /**
     * the singleton mode
     */
    private static final Cgp20ShufflePtoDesc INSTANCE = new Cgp20ShufflePtoDesc();

    /**
     * private constructor.
     */
    private Cgp20ShufflePtoDesc() {
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
