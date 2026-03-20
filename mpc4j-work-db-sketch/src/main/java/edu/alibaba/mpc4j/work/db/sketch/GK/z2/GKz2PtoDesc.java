package edu.alibaba.mpc4j.work.db.sketch.GK.z2;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * Protocol descriptor for the Z2 Boolean circuit implementation of GK (Greenwald-Khanna) sketch in the S³ framework.
 * 
 * This class provides the unique identifier and name for the GKz2 protocol implementation.
 * It follows the singleton pattern to ensure a single instance is used throughout the system.
 * 
 * The GKz2 protocol implements the Greenwald-Khanna sketch algorithm using Z2 Boolean circuits
 * for secure multi-party computation, enabling private quantile and rank queries on streaming data.
 * 
 * Reference: "Sketch-based Secure Query Processing for Streaming Data" (S³ framework)
 */
public class GKz2PtoDesc implements PtoDesc {
    /**
     * Unique protocol identifier for the GK Z2 implementation.
     * This ID is used to distinguish this protocol from other MPC protocols.
     */
    private static final int PTO_ID = Math.abs((int) -7344501423821829614L);
    
    /**
     * Protocol name for the GK Z2 implementation.
     * Indicates this is version 1 of the GK sketch using Z2 circuits.
     */
    private static final String PTO_NAME = "GK_V1";

    /**
     * Singleton instance of the protocol descriptor.
     * Ensures only one instance exists for protocol identification.
     */
    private static final GKz2PtoDesc INSTANCE = new GKz2PtoDesc();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private GKz2PtoDesc() {}

    /**
     * Gets the singleton instance of the GKz2 protocol descriptor.
     * 
     * @return the singleton instance
     */
    public static GKz2PtoDesc getInstance() {
        return INSTANCE;
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
