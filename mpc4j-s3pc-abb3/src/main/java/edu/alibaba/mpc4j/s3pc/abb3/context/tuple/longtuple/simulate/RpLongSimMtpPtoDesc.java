package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.simulate;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * information of replicated 3p sharing zl64 mt provider in simulate mode
 * the parties will generate multiplication tuples with common known keys once and reuse them in the future
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class RpLongSimMtpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4678681151193147497L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RP_Z2MT_SIMULATE_PROVIDER";

    /**
     * singleton mode
     */
    private static final RpLongSimMtpPtoDesc INSTANCE = new RpLongSimMtpPtoDesc();

    /**
     * private constructor
     */
    private RpLongSimMtpPtoDesc() {
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
