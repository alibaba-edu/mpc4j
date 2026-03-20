package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.sketch.SketchPartyPto;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;

/**
 * Count-Min Sketch (CMS) computing party interface in the S³ framework.
 *
 * <p>This interface defines the operations for CMS, a linear sketch used for frequency estimation in streaming data.
 * CMS maintains a log(1/δ)×s array where each row uses a different hash function h_i to map keys to [s].
 * The update operation is: CMS[i][h_i(k)] += v, and the query returns min{CMS[i][h_i(k)]}.</p>
 *
 * <p>In the S³ framework, CMS is implemented using secure multi-party computation (MPC) with Z2 Boolean circuits.
 * The Merge protocol processes buffered updates by: creating table T(id,value) → computing hash (h(k),1) → 
 * sorting → segmented prefix-sum → marking dummy → compacting → returning top s values.
 * The Query protocol computes h(q), retrieves CMS[h(q)] using multiplexer, and scans buffer for counts.</p>
 */
public interface CMSParty extends SketchPartyPto {
    /**
     * Updates the CMS sketch with new data.
     *
     * <p>This operation corresponds to the update step in CMS: CMS[i][h_i(k)] += v.
     * Data is initially added to a buffer and merged into the sketch table when the buffer is full,
     * following the Merge protocol in the S³ framework.</p>
     *
     * @param cmsTable the CMS sketch table to update
     * @param newData  the new data items to add (each item is a key-value pair)
     * @throws MpcAbortException if the protocol execution fails
     */
    void update(SketchTable cmsTable, MpcVector[] newData) throws MpcAbortException;

    /**
     * Performs a point query on the CMS sketch.
     *
     * <p>This operation implements the Query protocol in the S³ framework:
     * 1. Computes hash h(q) for the query key
     * 2. Retrieves CMS[h(q)] using multiplexer
     * 3. Scans the buffer to count matching keys
     * 4. Returns the minimum count across all hash rows: min{CMS[i][h_i(q)]}</p>
     *
     * @param cmsTable the CMS sketch table to query
     * @param queryData the query data (key to query)
     * @return the estimated frequency for the queried key
     * @throws MpcAbortException if the protocol execution fails
     */
    MpcVector[] getQuery(SketchTable cmsTable, MpcVector[] queryData) throws MpcAbortException;
}
