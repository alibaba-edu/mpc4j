package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.AbstractSketchPartyPto;

/**
 * Abstract base class for GK (Greenwald-Khanna) sketch parties in the S³ framework.
 * 
 * This abstract class provides common functionality for all GK sketch implementations,
 * including table management and protocol initialization. It extends the base sketch
 * party functionality with GK-specific data structures.
 * 
 * The GK sketch maintains tuples of (key, g1, g2, delta1, delta2) where:
 * - key: the sorted data value
 * - g1, g2: gap values tracking rank ranges
 * - delta1, delta2: uncertainty bounds for rank estimation
 * 
 * Reference: "Sketch-based Secure Query Processing for Streaming Data" (S³ framework)
 */
public abstract class AbstractGKParty extends AbstractSketchPartyPto {
    /**
     * The GK sketch table containing the sketch data.
     * This table stores the ordered tuples (k_i, g1_i, g2_i, delta1_i, delta2_i)
     * maintained by the GK algorithm for quantile and rank queries.
     */
    protected GKTable gkTable;

    /**
     * Constructs an abstract GK party with the specified protocol descriptor and configuration.
     * 
     * @param ptoDesc the protocol descriptor for this GK implementation
     * @param abb3Party the ABB3 party for secure MPC computation
     * @param config the GK protocol configuration
     */
    protected AbstractGKParty(PtoDesc ptoDesc, Abb3Party abb3Party, GKConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    /**
     * Sets the GK sketch table for this party.
     * 
     * @param gkTable the GK sketch table to use
     */
    public void setGkTable(GKTable gkTable) {
        this.gkTable = gkTable;
    }

    /**
     * Gets the current GK sketch table.
     * 
     * @return the GK sketch table containing sketch data
     */
    public GKTable getGkTable() {
        return gkTable;
    }

}