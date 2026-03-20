package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.AbstractSketchPartyPto;

/**
 * Abstract base class for Count-Min Sketch (CMS) computing parties in the S³ framework.
 * 
 * <p>This class provides common functionality for all CMS protocol implementations,
 * including management of the CMS table structure. It extends the abstract sketch party
 * and integrates with the ABB3 3-party MPC backend for secure computation.</p>
 * 
 * <p>Concrete implementations (e.g., CMSz2Party) extend this class to provide
 * protocol-specific logic for update and query operations using different MPC
 * backends (e.g., Z2 Boolean circuits).</p>
 */
public abstract class AbstractCMSParty extends AbstractSketchPartyPto {
    /**
     * The CMS sketch table structure.
     * 
     * <p>This table holds the CMS data structure, which consists of:
     * - The sketch table: a log(1/δ)×s array storing frequency counts
     * - The buffer: a temporary storage for incoming updates before merging
     * - Hash parameters: parameters for the hash functions used in CMS</p>
     */
    protected AbstractCMSTable cmsTable;

    /**
     * Constructs an abstract CMS computing party.
     * 
     * @param ptoDesc   the protocol descriptor for this CMS implementation
     * @param abb3Party the ABB3 party providing the underlying 3-party MPC functionality
     * @param config    the CMS configuration specifying protocol parameters
     */
    protected AbstractCMSParty(PtoDesc ptoDesc, Abb3Party abb3Party, CMSConfig config) {
        super(ptoDesc, abb3Party, config);
    }
}
