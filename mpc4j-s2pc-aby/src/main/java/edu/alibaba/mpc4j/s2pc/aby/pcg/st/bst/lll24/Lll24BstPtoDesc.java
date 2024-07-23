package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LLL24-BST protocol description.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
class Lll24BstPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2730092296251912800L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLL24_BST";

    /**
     * private constructor.
     */
    private Lll24BstPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Lll24BstPtoDesc INSTANCE = new Lll24BstPtoDesc();

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
