package edu.alibaba.mpc4j.work.db.sketch.utils.hll;

import java.math.BigInteger;

/**
 * Interface for HyperLogLog (HLL) cardinality estimation implementations.
 * HLL is a probabilistic data structure for estimating the number of distinct elements.
 */
public interface HLL {
    /**
     * Inserts a single element into the sketch
     * @param element element to insert
     */
    void input(BigInteger element);
    
    /**
     * Inserts multiple elements into the sketch
     * @param elements elements to insert
     */
    void input(BigInteger... elements);
    
    /**
     * Queries the estimated cardinality
     * @return estimated number of distinct elements
     */
    double query();
}
