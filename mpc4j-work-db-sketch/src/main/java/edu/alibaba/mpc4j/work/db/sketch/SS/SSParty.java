package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.sketch.SketchPartyPto;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;

/**
 * SpaceSaving (SS) sketch computing party interface for the S³ framework.
 *
 * <p>SpaceSaving is a count-based sketch algorithm used for top-k queries and frequency estimation
 * in streaming data scenarios. It maintains at most s (key, value) pairs in a set SSS.
 *
 * <p><b>Update Operation:</b> If the key exists, increment its value; if not and space is available,
 * add (key, 1); if full, replace the key with minimum value and increment its counter.
 *
 * <p><b>Query Operation:</b> Returns the top c key-value pairs with highest frequencies.
 *
 * <p><b>Merge Protocol (Algorithm 4):</b> Create table T(key,value) → Add buffer keys as (key,1)
 * → Sort by key → Segmented prefix-sum aggregation → Mark dummy entries → Compact → Sort by value
 * → Return top s entries.
 *
 * <p>This interface defines the core operations for SS sketch in MPC settings using Z2 Boolean circuits.
 */
public interface SSParty extends SketchPartyPto {
    /**
     * Update the sketch or add new data into buffer.
     *
     * <p>When the buffer reaches capacity (size s), this triggers the Merge protocol
     * which flushes the buffer into the sketch table following Algorithm 4:
     * 1. Merge buffer data with existing sketch table
     * 2. Sort by key to group identical keys
     * 3. Perform group-by aggregation to sum frequencies
     * 4. Compact by keeping only top s entries
     *
     * @param mgTable the SS sketch table containing key-value pairs
     * @param newData new streaming data to be added (single key-value pair)
     * @throws MpcAbortException the protocol failure abort exception
     */
    void update(SketchTable mgTable, MpcVector[] newData) throws MpcAbortException;

    /**
     * Get the result of top-k query from the sketch.
     *
     * <p>The Query protocol follows these steps:
     * 1. First, execute Merge protocol to flush any remaining buffer data
     * 2. Sort entries by value (frequency) in descending order
     * 3. Select and return the top k entries
     *
     * @param mgTable the SS sketch table containing key-value pairs
     * @param k the number of top-frequency items to return
     * @return query result array where [keys, values] represent top-k key-value pairs
     * @throws MpcAbortException the protocol failure abort exception
     */
    MpcVector[] getQuery(SketchTable mgTable, int k) throws MpcAbortException;
}
