package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.file;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * information of replicated 3p sharing z2 mt provider in file mode
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class RpLongFileMtpPtoDesc implements PtoDesc{
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4560118115956269621L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RP_ZL64MT_FILE_PROVIDER";

    /**
     * singleton mode
     */
    private static final RpLongFileMtpPtoDesc INSTANCE = new RpLongFileMtpPtoDesc();

    /**
     * private constructor
     */
    private RpLongFileMtpPtoDesc() {
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
