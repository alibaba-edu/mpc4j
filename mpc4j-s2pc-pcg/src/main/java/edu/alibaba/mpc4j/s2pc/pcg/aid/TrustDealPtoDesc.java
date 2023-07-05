package edu.alibaba.mpc4j.s2pc.pcg.aid;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * trust deal protocol description.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class TrustDealPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3652397097644375696L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "TRUST_DEALER_AID";

    /**
     * protocol step
     */
    public enum AidPtoStep {
        /**
         * init query
         */
        INIT_QUERY,
        /**
         * init response
         */
        INIT_RESPONSE,
        /**
         * request query
         */
        REQUEST_QUERY,
        /**
         * request response
         */
        REQUEST_RESPONSE,
        /**
         * destroy query
         */
        DESTROY_QUERY,
        /**
         * destroy response
         */
        DESTROY_RESPONSE,
    }

    /**
     * singleton mode
     */
    private static final TrustDealPtoDesc INSTANCE = new TrustDealPtoDesc();

    /**
     * private constructor.
     */
    private TrustDealPtoDesc() {
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
