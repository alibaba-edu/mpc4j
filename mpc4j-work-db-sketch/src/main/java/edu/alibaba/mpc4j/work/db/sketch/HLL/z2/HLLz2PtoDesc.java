package edu.alibaba.mpc4j.work.db.sketch.HLL.z2;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * Protocol description for Z2 Boolean circuit-based HyperLogLog (HLL) in the S³ Framework.
 * 
 * This class provides metadata and identification for the HLL protocol implementation.
 * It follows the singleton pattern to ensure a single instance per protocol type.
 * 
 * The protocol ID is a unique identifier used for protocol registration and lookup.
 * The protocol name is used for logging and debugging purposes.
 */
public class HLLz2PtoDesc implements PtoDesc {
    /**
     * Unique protocol identifier.
     * This ID is used to register and identify the HLL protocol within the MPC framework.
     * The value is derived from a hash to ensure uniqueness.
     */
    private static final int PTO_ID = Math.abs((int)486175662185756613L);

    /**
     * Protocol name.
     * Used for logging, debugging, and protocol identification.
     */
    private static final String PTO_NAME = "HLLPto";

    /**
     * Singleton instance of the protocol description.
     * Ensures only one instance exists per protocol type.
     */
    private static final HLLz2PtoDesc INSTANCE = new HLLz2PtoDesc();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private HLLz2PtoDesc() {}

    /**
     * Gets the singleton instance of the protocol description.
     * 
     * @return the HLLz2PtoDesc instance
     */
    public static HLLz2PtoDesc getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the unique protocol identifier.
     * 
     * @return the protocol ID
     */
    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    /**
     * Gets the protocol name.
     * 
     * @return the protocol name
     */
    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
