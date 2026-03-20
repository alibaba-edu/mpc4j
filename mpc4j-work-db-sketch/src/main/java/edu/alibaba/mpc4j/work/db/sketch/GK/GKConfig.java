package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * GK (Greenwald-Khanna) sketch protocol configuration interface.
 * 
 * This interface defines the configuration parameters for GK sketch implementations
 * in the S³ framework. Different implementations (e.g., Z2 Boolean circuit) can
 * provide their specific configurations through this interface.
 * 
 * The configuration includes protocol type selection and security model settings
 * for the MPC-based GK sketch computation.
 */
public interface GKConfig extends MultiPartyPtoConfig  {
    /**
     * Get the protocol type for this GK sketch implementation.
     * 
     * @return the protocol type (e.g., Z2 for Boolean circuit implementation)
     */
    GKFactory.GKPtoType getPtoType();
}
