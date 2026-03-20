package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLFactory.HLLPtoType;

/**
 * Configuration interface for HyperLogLog (HLL) protocol in the S³ Framework.
 * 
 * This interface defines the configuration parameters for HLL sketch implementation.
 * Different implementations (e.g., Z2 Boolean circuit version) can provide different configurations
 * for security model, performance optimizations, and underlying cryptographic primitives.
 * 
 * Currently supports:
 * - Z2: Implementation using Z2 Boolean circuits for secure computation
 */
public interface HLLConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type for HLL implementation.
     * 
     * @return the HLL protocol type (e.g., Z2 for Boolean circuit implementation)
     */
    HLLPtoType getPtoType();

}
