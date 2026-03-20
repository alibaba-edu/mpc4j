package edu.alibaba.mpc4j.work.db.sketch.utils.gk;

import java.math.BigInteger;

/**
 * Interface for Greenwald-Khanna (GK) quantile sketch implementations.
 * GK is a deterministic algorithm for computing approximate quantiles from a data stream.
 */
public interface GK {
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
     * Queries the estimated rank of an element
     * @param element element to query
     * @return estimated rank
     */
    BigInteger query(BigInteger element);
}
