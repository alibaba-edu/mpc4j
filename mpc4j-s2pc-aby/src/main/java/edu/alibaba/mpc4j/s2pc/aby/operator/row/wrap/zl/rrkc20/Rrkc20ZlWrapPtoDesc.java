package edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.rrkc20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RRKC20 Zl wrap protocol description. The protocol comes from the following paper:
 * <p>
 * Rathee, Deevashwer, Mayank Rathee, Nishant Kumar, Nishanth Chandran, Divya Gupta, Aseem Rastogi, and Rahul Sharma.
 * CrypTFlow2: Practical 2-party secure inference. CCS 2020, pp. 325-342. 2020.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrkc20ZlWrapPtoDesc implements PtoDesc {
    /**
     * protocol id
     */
    private static final int PTO_ID = Math.abs((int) 4470148822713392173L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RRKC20_ZL_WRAP";

    /**
     * singleton mode
     */
    private static final Rrkc20ZlWrapPtoDesc INSTANCE = new Rrkc20ZlWrapPtoDesc();

    /**
     * private constructor
     */
    private Rrkc20ZlWrapPtoDesc() {
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
