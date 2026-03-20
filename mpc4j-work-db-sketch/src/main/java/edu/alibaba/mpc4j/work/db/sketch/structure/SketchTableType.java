package edu.alibaba.mpc4j.work.db.sketch.structure;

/**
 * Enumeration of sketch table types supported in the S³ framework.
 * <p>
 * The sketches are categorized into two groups:
 * <ul>
 *   <li><b>Linear sketches</b>: CMS and HLL, which apply linear transformations on data streams.</li>
 *   <li><b>Count-based sketches</b>: SS and GK, which store a small subset of entities from data streams.</li>
 * </ul>
 */
public enum SketchTableType {
    /**
     * Count-Min Sketch (CMS): a linear sketch for frequency estimation.
     * Supports look-up queries that return the approximate total value associated with a given key.
     * Uses hash-based indexing and prefix-sum aggregation in the Merge protocol.
     */
    CMS,
    /**
     * HyperLogLog (HLL): a linear sketch for distinct count (cardinality estimation).
     * Approximates the number of distinct keys using LeadingOnes hash and prefix-max aggregation.
     */
    HLL,
    /**
     * SpaceSaving (SS): a count-based sketch for top-k and frequency estimation.
     * Maintains at most s key-value pairs sorted by frequency, replacing the minimum when full.
     */
    SS,
    /**
     * Greenwald-Khanna (GK): a count-based sketch for quantile and rank queries.
     * Maintains a sorted summary of (key, g1, g2, delta1, delta2) tuples with bounded error.
     */
    GK
}
