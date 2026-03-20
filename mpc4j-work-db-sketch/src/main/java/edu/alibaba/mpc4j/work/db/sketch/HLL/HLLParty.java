package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.db.sketch.SketchPartyPto;

/**
 * HyperLogLog (HLL) Party Interface for the S³ Framework.
 * 
 * This interface defines the core operations for HyperLogLog sketch in secure multi-party computation.
 * HLL is a linear sketch used for distinct count/cardinality estimation.
 * 
 * Algorithm Overview (from the paper):
 * - HLL maintains an array of s counters SHLL, initialized to zeros
 * - Uses two hash functions: h1 maps key to [s], h2 maps key to [2^l]
 * - Update: SHLL[h1(k)] = max(SHLL[h1(k)], LeadingOnes(h2(k)))
 * - LeadingOnes: returns the count of consecutive leading 1's in binary representation
 * - Query: uses LogLog estimator, computes sum(SHLL), then post-processes in plaintext to get cardinality estimate
 * - Merge protocol: creates table T(id,value) → computes (h1(k), LeadingOnes(h2(k))) for buffered keys → sorts → segmented prefix-max → marks dummy → compact
 * 
 * In MPC, this is implemented using Z2 Boolean circuits.
 */
public interface HLLParty extends SketchPartyPto {

    /**
     * Updates the HLL sketch with a batch of elements.
     * 
     * This implements the update operation: SHLL[h1(k)] = max(SHLL[h1(k)], LeadingOnes(h2(k)))
     * Elements are first added to a buffer. When the buffer is full, the merge protocol is triggered.
     * The merge protocol performs: hash computation → sort → segmented prefix-max → compaction
     * 
     * @param hllTable the HLL sketch table containing the counters and buffer
     * @param elements a batch of elements to update, the number can be arbitrary
     * @throws MpcAbortException if an error occurs during MPC computation (e.g., from lowmc.enc)
     */
    void update(AbstractHLLTable hllTable, MpcVector[] elements) throws MpcAbortException;

    /**
     * Queries the HLL sketch to compute the sum of all counters.
     * 
     * This implements the query operation for cardinality estimation.
     * Computes sum(SHLL) using LogLog estimator. The result is then post-processed in plaintext
     * to obtain the final cardinality estimate: r̃ = α'm * s * 2^(r/s)
     * 
     * If the buffer is not empty, merge is triggered before query to ensure all updates are applied.
     * 
     * @param hllTable the HLL sketch table to query
     * @return the sum of all counters as TripletZ2Vector array, representing the raw estimate
     * @throws MpcAbortException if an error occurs during MPC computation
     */
    TripletZ2Vector[] query(AbstractHLLTable hllTable) throws MpcAbortException;
}
