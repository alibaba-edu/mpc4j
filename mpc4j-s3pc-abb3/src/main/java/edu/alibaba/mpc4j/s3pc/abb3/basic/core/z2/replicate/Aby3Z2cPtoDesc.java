package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Information of aby3 z2c protocols
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Aby3Z2cPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 487183625584490767L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ABY3_BC";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * initialize
         */
        INIT,
        /**
         * input the share
         */
        INPUT_SHARE,
        /**
         * reveal the share to the specific party
         */
        REVEAL_SHARE,
        /**
         * open the share to all parties
         */
        OPEN_SHARE,
        /**
         * and operation
         */
        AND_OP,
        /**
         * compare view
         */
        COMPARE_VIEW,
    }

    /**
     * singleton mode
     */
    private static final Aby3Z2cPtoDesc INSTANCE = new Aby3Z2cPtoDesc();

    /**
     * private constructor
     */
    private Aby3Z2cPtoDesc() {
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
