package edu.alibaba.mpc4j.work.db.sketch.utils.pop;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopFactory.PopPtoType;

/**
 * Configuration interface for Pop (Permute-and-Open) protocol.
 * Defines the protocol type and security parameters for the Pop operation.
 */
public interface PopConfig extends MultiPartyPtoConfig {
    /**
     * Get the protocol type
     *
     * @return the protocol type (e.g., NAIVE)
     */
    PopPtoType getPtoType();
}
