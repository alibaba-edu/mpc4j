package edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RRK+20 Zl Max2 protocol description. The protocol comes from Section 5.2.2 of the following paper:
 * <p>
 * Rathee, Deevashwer, Mayank Rathee, Nishant Kumar, Nishanth Chandran, Divya Gupta, Aseem Rastogi, and Rahul Sharma.
 * CrypTFlow2: Practical 2-party secure inference. CCS 2020, pp. 325-342. 2020.
 * </p>
 *
 * @author Li Peng
 * @date 2023/5/22
 */
class Rrk20ZlMax2PtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 179702838604202322L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RRK+20_ZL_MAX2";
    /**
     * singleton mode
     */
    private static final Rrk20ZlMax2PtoDesc INSTANCE = new Rrk20ZlMax2PtoDesc();

    /**
     * private constructor.
     */
    private Rrk20ZlMax2PtoDesc() {
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
