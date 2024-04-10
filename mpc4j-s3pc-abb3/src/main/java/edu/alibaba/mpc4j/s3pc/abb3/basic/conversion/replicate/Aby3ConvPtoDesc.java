package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Information of aby3 type conversion protocols
 *
 * @author Feng Han
 * @date 2024/01/17
 */
public class Aby3ConvPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -2298142936717584505L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ABY3_TYPE_CONV";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * initialize
         */
        A_MUL_BIT,
    }

    /**
     * singleton mode
     */
    private static final Aby3ConvPtoDesc INSTANCE = new Aby3ConvPtoDesc();

    /**
     * private constructor
     */
    private Aby3ConvPtoDesc() {
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
