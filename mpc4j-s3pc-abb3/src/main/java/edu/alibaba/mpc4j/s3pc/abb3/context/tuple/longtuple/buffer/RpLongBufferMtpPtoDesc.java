package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.buffer;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * information of replicated 3p sharing zl64 mt provider in buffer mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpLongBufferMtpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5825882195417408122L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RP_Z2MT_BUFFER_PROVIDER";

    /**
     * singleton mode
     */
    private static final RpLongBufferMtpPtoDesc INSTANCE = new RpLongBufferMtpPtoDesc();

    /**
     * private constructor
     */
    private RpLongBufferMtpPtoDesc() {
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
