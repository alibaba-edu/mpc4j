package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.CMS.z2.CMSz2Config;
import edu.alibaba.mpc4j.work.db.sketch.CMS.z2.CMSz2Party;

/**
 * Factory class for creating Count-Min Sketch (CMS) protocol parties in the S³ framework.
 * 
 * <p>This factory provides a centralized way to instantiate CMS party implementations
 * based on the specified protocol type and configuration. It supports different MPC
 * backends, with the Z2 Boolean circuit implementation being the primary variant.</p>
 */
public class CMSFactory {
    /**
     * Enumeration of available CMS protocol types in the S³ framework.
     * 
     * <p>Each protocol type corresponds to a different MPC backend implementation:
     * - CMS_Z2: Z2 Boolean circuit implementation for secure CMS operations</p>
     */
    public enum CMSPtoType {
        /**
         * Z2 Boolean circuit implementation of CMS.
         * Uses Z2 arithmetic for secure computation of CMS operations.
         */
        CMS_Z2,
    }

    /**
     * Creates a CMS computing party based on the specified configuration.
     * 
     * <p>This factory method instantiates the appropriate CMS party implementation
     * based on the protocol type specified in the configuration. The party is
     * initialized with the provided ABB3 party (3-party MPC backend).</p>
     *
     * @param abb3Party the ABB3 party providing the underlying 3-party MPC functionality
     * @param config    the CMS configuration specifying protocol type and parameters
     * @return a CMS computing party instance
     * @throws IllegalArgumentException if an invalid protocol type is specified
     */
    public static CMSParty createParty(Abb3Party abb3Party, CMSConfig config) {
        return switch (config.getPtoType()) {
            case CMS_Z2 -> new CMSz2Party(abb3Party, (CMSz2Config) config);
            default -> throw new IllegalArgumentException("Invalid config.getPtoType() in creating CMSParty");
        };
    }

}
