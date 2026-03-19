package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLFactory.HLLPtoType;

public interface HLLConfig extends MultiPartyPtoConfig {
    HLLPtoType getPtoType();

}
