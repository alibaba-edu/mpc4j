package edu.alibaba.mpc4j.work.db.sketch.SS.v1;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * v1 MG protocol description.
 */
public class v1SSPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6114892383607501287L);

    /**
     * protocol name
     */
    private static final String PTO_NAME = "MG_V1";

    /**
     * singleton mode
     */
    private static final v1SSPtoDesc INSTANCE = new v1SSPtoDesc();

    /**
     * private constructor
     */
    private v1SSPtoDesc() {}

    public static v1SSPtoDesc getInstance() {
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
