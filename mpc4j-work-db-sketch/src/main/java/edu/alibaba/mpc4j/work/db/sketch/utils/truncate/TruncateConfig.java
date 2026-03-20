package edu.alibaba.mpc4j.work.db.sketch.utils.truncate;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateFactory.TruncatePtoType;

/**
 * Configuration interface for Truncate protocol.
 * Defines the protocol type and security parameters for the truncate operation.
 */
public interface TruncateConfig extends MultiPartyPtoConfig {
    /**
     * Get the protocol type
     *
     * @return the protocol type (e.g., EXT)
     */
    TruncatePtoType getPtoType();
}
