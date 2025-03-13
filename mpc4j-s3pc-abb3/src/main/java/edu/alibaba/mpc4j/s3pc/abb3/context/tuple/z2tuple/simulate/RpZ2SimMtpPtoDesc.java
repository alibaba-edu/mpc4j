package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.simulate;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * @author Feng Han
 * @date 2025/2/17
 */
public class RpZ2SimMtpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -8026101871413911170L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RP_Z2MT_SIMULATE_PROVIDER";

    /**
     * singleton mode
     */
    private static final RpZ2SimMtpPtoDesc INSTANCE = new RpZ2SimMtpPtoDesc();

    /**
     * private constructor
     */
    private RpZ2SimMtpPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    /**
     * protocol step
     */
    protected enum PtoStep {
        /**
         * send zero shares
         */
        SEND_ZERO_SHARE,
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
