package edu.alibaba.mpc4j.s3pc.abb3.basic;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Information of abb3 protocols
 *
 * @author Feng Han
 * @date 2024/01/26
 */
public class Abb3RpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4473458444028730024L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ABB3_RP_PARTY";

    /**
     * singleton mode
     */
    private static final Abb3RpPtoDesc INSTANCE = new Abb3RpPtoDesc();

    /**
     * private constructor
     */
    private Abb3RpPtoDesc() {
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
