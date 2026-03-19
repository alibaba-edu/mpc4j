package edu.alibaba.mpc4j.work.db.sketch.utils.truncate;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateFactory.TruncatePtoType;

public interface TruncateConfig extends MultiPartyPtoConfig {
    /**
     * get the protocol type
     *
     * @return the protocol type
     */
    TruncatePtoType getPtoType();
}
