package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * MG config.
 */
public interface SSConfig extends MultiPartyPtoConfig {
    /**
     * get the protocol type
     *
     * @return the protocol type
     */
    SSFactory.MGPtoType getPtoType();
}
