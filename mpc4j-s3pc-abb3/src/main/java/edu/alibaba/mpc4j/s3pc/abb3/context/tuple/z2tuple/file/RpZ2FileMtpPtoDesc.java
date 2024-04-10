package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.file;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * information of replicated 3p sharing z2 mt provider in file mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpZ2FileMtpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 427813024470528020L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RP_Z2MT_FILE_PROVIDER";

    /**
     * singleton mode
     */
    private static final RpZ2FileMtpPtoDesc INSTANCE = new RpZ2FileMtpPtoDesc();

    /**
     * private constructor
     */
    private RpZ2FileMtpPtoDesc() {
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
