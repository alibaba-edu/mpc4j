package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSFactory.SSPtoType;

/**
 * SpaceSaving (SS) sketch protocol configuration interface.
 * 
 * <p>This configuration defines the parameters and protocol type for SS sketch
 * implementation in the S³ framework. It extends the multi-party protocol configuration
 * and specifies which MPC implementation variant (e.g., Z2 Boolean circuit) to use.
 * 
 * <p>The configuration includes settings for:
 * - Security model (semi-honest or malicious)
 * - Sub-protocol configurations (permutation, sorting, aggregation)
 * - Circuit implementation details
 */
public interface SSConfig extends MultiPartyPtoConfig {
    /**
     * Get the protocol type for SS sketch implementation.
     * 
     * @return the protocol type (e.g., Z2 for Boolean circuit implementation)
     */
    SSPtoType getPtoType();
}
