package edu.alibaba.mpc4j.work.db.sketch.CMS.z2;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * Protocol descriptor for the Z2 Boolean circuit implementation of Count-Min Sketch (CMS) in the S³ framework.
 * 
 * <p>This descriptor provides metadata for the CMSz2Party protocol, including a unique protocol ID
 * and name. The protocol implements secure CMS operations (update and query) using Z2 Boolean circuits.</p>
 * 
 * <p>The CMSz2 protocol follows the paper's Merge and Query protocols:
 * - Merge: hash → sort → segmented prefix-sum → mark dummy → compact
 * - Query: compute hash → multiplexer retrieval → buffer scan</p>
 * 
 * <p>This class follows the singleton pattern to ensure a single descriptor instance.</p>
 */
public class CMSz2PtoDesc implements PtoDesc {
    /**
     * Unique protocol identifier for CMS_Z2.
     * 
     * <p>This ID is used to distinguish this protocol from other MPC protocols
     * in the framework.</p>
     */
    private static final int PTO_ID = Math.abs((int) 8731289488896742374L);
    /**
     * Human-readable protocol name.
     * 
     * <p>The name "CMS_Z2" indicates Count-Min Sketch with Z2 Boolean circuit implementation.</p>
     */
    private static final String PTO_NAME = "CMS_Z2";

    /**
     * Singleton instance of the protocol descriptor.
     * 
     * <p>Ensures that only one instance exists per JVM.</p>
     */
    private static final CMSz2PtoDesc INSTANCE = new CMSz2PtoDesc();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private CMSz2PtoDesc() {}

    /**
     * Gets the singleton instance of the protocol descriptor.
     * 
     * @return the CMSz2PtoDesc instance
     */
    public static CMSz2PtoDesc getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the unique protocol ID.
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
     * @return the protocol name "CMS_Z2"
     */
    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
