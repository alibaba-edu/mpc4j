package edu.alibaba.mpc4j.work.db.sketch.SS.z2;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * SpaceSaving (SS) sketch protocol description for Z2 Boolean circuit implementation.
 * 
 * <p>This class provides metadata and identification for the SS protocol using Z2 circuits
 * in the S³ framework. It implements the singleton pattern to ensure a single instance
 * of the protocol description.
 * 
 * <p>The protocol implements Algorithm 4 (Merge) and the Query protocol from the paper,
 * using Z2 Boolean circuits for secure multi-party computation.
 */
public class SSz2PtoDesc implements PtoDesc {
    /**
     * Unique protocol identifier.
     * 
     * <p>This ID is used to distinguish the SS_Z2 protocol from other protocols
     * in the MPC framework. The hash is derived from a unique seed value.
     */
    private static final int PTO_ID = Math.abs((int) 6114892383607501287L);

    /**
     * Protocol name.
     * 
     * <p>Human-readable name identifying this as the SpaceSaving protocol
     * using Z2 Boolean circuit implementation.
     */
    private static final String PTO_NAME = "SS_Z2";

    /**
     * Singleton instance of the protocol description.
     * 
     * <p>Ensures only one instance exists throughout the application lifecycle.
     */
    private static final SSz2PtoDesc INSTANCE = new SSz2PtoDesc();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private SSz2PtoDesc() {}

    /**
     * Get the singleton instance of SSz2PtoDesc.
     *
     * @return the singleton instance
     */
    public static SSz2PtoDesc getInstance() {
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
