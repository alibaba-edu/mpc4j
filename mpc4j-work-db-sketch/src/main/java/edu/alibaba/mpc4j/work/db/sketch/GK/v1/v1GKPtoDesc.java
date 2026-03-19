package edu.alibaba.mpc4j.work.db.sketch.GK.v1;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

public class v1GKPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -7344501423821829614L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GK_V1";

    /**
     * singleton mode
     */
    private static final v1GKPtoDesc INSTANCE = new v1GKPtoDesc();

    /**
     * private constructor
     */
    private v1GKPtoDesc() {}

    public static v1GKPtoDesc getInstance() {
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
