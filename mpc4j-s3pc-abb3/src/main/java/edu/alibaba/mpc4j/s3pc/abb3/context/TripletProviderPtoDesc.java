package edu.alibaba.mpc4j.s3pc.abb3.context;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * information of provider for 3p sharing
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class TripletProviderPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4272167557705064584L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ABB3_CONTEXT";

    /**
     * singleton mode
     */
    private static final TripletProviderPtoDesc INSTANCE = new TripletProviderPtoDesc();

    /**
     * private constructor
     */
    private TripletProviderPtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    protected enum PtoStep {
        /**
         * initialize the context
         */
        INIT,
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
