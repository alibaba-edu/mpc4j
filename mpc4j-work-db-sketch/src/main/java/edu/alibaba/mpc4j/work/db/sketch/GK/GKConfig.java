package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * GK config.
 */
public interface GKConfig extends MultiPartyPtoConfig  {
    /**
     * get the protocol type
     *
     * @return the protocol type
     */
    GKFactory.GKPtoType getPtoType();
}
