package edu.alibaba.mpc4j.work.db.sketch.HLL.v1;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

public class v1HLLPtoDesc implements PtoDesc {
    private static final int PTO_ID = Math.abs((int)486175662185756613L);

    private static final String PTO_NAME = "HLLPto";

    private static final v1HLLPtoDesc INSTANCE = new v1HLLPtoDesc();

    private v1HLLPtoDesc() {}

    public static v1HLLPtoDesc getInstance() {
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
