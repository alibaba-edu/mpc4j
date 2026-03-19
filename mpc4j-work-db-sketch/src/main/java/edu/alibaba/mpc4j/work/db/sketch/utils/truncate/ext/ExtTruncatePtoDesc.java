package edu.alibaba.mpc4j.work.db.sketch.utils.truncate.ext;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class ExtTruncatePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3772001954012189298L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "TRUNCATE_EXT";

    /**
     * singleton mode
     */
    private static final ExtTruncatePtoDesc INSTANCE = new ExtTruncatePtoDesc();

    /**
     * private constructor
     */
    private ExtTruncatePtoDesc() {
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
