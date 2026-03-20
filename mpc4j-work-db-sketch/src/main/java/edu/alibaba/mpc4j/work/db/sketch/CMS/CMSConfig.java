package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSFactory.CMSPtoType;

/**
 * Configuration interface for Count-Min Sketch (CMS) protocols in the S³ framework.
 * 
 * <p>This interface defines the configuration parameters for CMS implementations,
 * which support different MPC backends (e.g., Z2 Boolean circuits) and security models.
 * The configuration determines which specific protocol variant will be used for
 * secure CMS operations (update and query).</p>
 */
public interface CMSConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type for this CMS configuration.
     * 
     * <p>The protocol type determines which MPC backend and implementation
     * will be used (e.g., CMS_Z2 for Z2 Boolean circuit implementation).</p>
     *
     * @return the CMS protocol type
     */
    CMSPtoType getPtoType();
}
