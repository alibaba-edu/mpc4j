package edu.alibaba.mpc4j.work.db.sketch.utils.pop;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopFactory.PopPtoType;

/**
 * pop protocol config
 */
public interface PopConfig extends MultiPartyPtoConfig {
    /**
     * get the protocol type
     *
     * @return the protocol type
     */
    PopPtoType getPtoType();
}
