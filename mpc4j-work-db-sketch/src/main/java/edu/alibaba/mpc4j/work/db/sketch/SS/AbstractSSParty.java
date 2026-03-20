package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.AbstractSketchPartyPto;

/**
 * Abstract SpaceSaving (SS) sketch computing party.
 * 
 * <p>This abstract class provides the base implementation for SS sketch parties
 * in the S³ framework. It extends the generic sketch party functionality and
 * manages the SS sketch table structure.
 * 
 * <p>The class handles:
 * - Initialization of SS protocol with ABB3 party
 * - Management of the SS sketch table (key-value pairs)
 * - Common functionality for SS operations (update, query)
 */
public abstract class AbstractSSParty extends AbstractSketchPartyPto {
    /**
     * The SS sketch table containing key-value pairs.
     * 
     * <p>This table maintains at most s entries, where each entry consists of:
     * - Key: the item identifier (e.g., stream element)
     * - Value: the estimated frequency/count of the key
     * 
     * <p>The table is updated through the Merge protocol when the buffer fills,
     * ensuring space-efficient storage while maintaining frequency estimates.
     */
    protected AbstractSSTable ssTable;

    /**
     * Constructor for abstract SS party.
     *
     * @param ptoDesc   the protocol description
     * @param abb3Party the ABB3 (3-party arithmetic) party for MPC operations
     * @param config    the SS protocol configuration
     */
    protected AbstractSSParty(PtoDesc ptoDesc, Abb3Party abb3Party, SSConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    /**
     * Set the SS sketch table for this party.
     * 
     * <p>This method associates a specific sketch table instance with the party,
     * allowing the party to perform operations on the table.
     *
     * @param cmsTable the SS sketch table to be set
     */
    public void setssTable(AbstractSSTable cmsTable) {
        this.ssTable = cmsTable;
    }
}
