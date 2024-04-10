package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.buffer;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * information of replicated 3p sharing z2 mt provider in buffer mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpZ2BufferMtpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1484517155673108842L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RP_Z2MT_BUFFER_PROVIDER";

    /**
     * singleton mode
     */
    private static final RpZ2BufferMtpPtoDesc INSTANCE = new RpZ2BufferMtpPtoDesc();

    /**
     * private constructor
     */
    private RpZ2BufferMtpPtoDesc() {
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
