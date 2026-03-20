package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.sketch.SketchPartyPto;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;

/**
 * GK (Greenwald-Khanna) computing party interface in the S³ framework.
 * 
 * This interface defines the operations for the Greenwald-Khanna sketch algorithm,
 * which is a count-based sketch used for quantile and rank queries on streaming data.
 * The GK sketch maintains an ordered set of tuples S_GK = {(k_i, g1_i, g2_i, delta1_i, delta2_i)} where:
 * - k_i: sorted key value
 * - g1_i, g2_i: two gap values tracking rank ranges
 * - delta1_i, delta2_i: two delta values representing rank uncertainty
 * 
 * The algorithm supports two main operations:
 * 1. Update: Insert new elements into the correct sorted position
 * 2. Query: Given rank r, return key with rank in [r-ε*n, r+ε*n]
 * 
 * The running size s' = 2s*ln(n/s + 2) + 2 dynamically grows with stream size n.
 * 
 * Reference: "Sketch-based Secure Query Processing for Streaming Data" (S³ framework)
 */
public interface GKParty extends SketchPartyPto {
    /**
     * Update the sketch or add new data into buffer.
     * 
     * This method implements the update operation of the GK algorithm.
     * New elements are added to a buffer first, and when the buffer is full,
     * the merge protocol is triggered:
     * - Create table T(key, g1, g2, delta1, delta2, t, dummy)
     * - Add buffer elements to the table
     * - Sort by key
     * - Calculate gaps and deltas
     * - Compact the table
     *
     * @param gkTable gk table containing the sketch data
     * @param newData  new data to be inserted (key value)
     * @throws MpcAbortException the protocol failure abort exception
     */
    void update(SketchTable gkTable, MpcVector[] newData) throws MpcAbortException;

    /**
     * Get the result of quantile query.
     * 
     * This method implements the query operation of the GK algorithm.
     * Given a query rank r, it returns the key whose rank is in [r-ε*n, r+ε*n].
     * The query searches both the sketch table and the buffer.
     *
     * @param gkTable gk table containing the sketch data
     * @param queryData query data (rank value)
     * @return query result (key value)
     * @throws MpcAbortException the protocol failure abort exception
     */
    MpcVector[] getQuery(SketchTable gkTable, MpcVector[] queryData) throws MpcAbortException;
}
