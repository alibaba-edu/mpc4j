package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGP20-BST protocol description. The construction comes from the following paper:
 * <p>
 * Chase, Melissa, Esha Ghosh, and Oxana Poburinnaya. Secret-shared shuffle. ASIACRYPT 2020, pp. 342-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/4/24
 */
class Cgp20BstPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6812152317879848761L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGP20_BST";

    /**
     * private constructor.
     */
    private Cgp20BstPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Cgp20BstPtoDesc INSTANCE = new Cgp20BstPtoDesc();

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
