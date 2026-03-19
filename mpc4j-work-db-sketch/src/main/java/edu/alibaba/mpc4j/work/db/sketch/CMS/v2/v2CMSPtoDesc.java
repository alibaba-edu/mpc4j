package edu.alibaba.mpc4j.work.db.sketch.CMS.v2;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * v2 CMS protocol
 */
public class v2CMSPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8731289488896742374L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CMS_V2";

    /**
     * singleton mode
     */
    private static final v2CMSPtoDesc INSTANCE = new v2CMSPtoDesc();

    /**
     * private constructor
     */
    private v2CMSPtoDesc() {}

    public static v2CMSPtoDesc getInstance() {
        return INSTANCE;
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
