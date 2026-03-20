package edu.alibaba.mpc4j.work.db.sketch.utils.ss;

import java.math.BigInteger;
import java.util.Map;

/**
 * Interface for Space-Saving (SS) sketch implementations.
 * SS is a probabilistic data structure for finding top-k frequent items.
 */
public interface SS {
    /**
     * Inserts multiple elements with weight 1
     * @param elements elements to insert
     */
    void input(BigInteger... elements);
    
    /**
     * Inserts an element with specified weight
     * @param element element to insert
     * @param weight weight of the element
     */
    void input(BigInteger element,BigInteger weight);
    
    /**
     * Inserts a single element with weight 1
     * @param element element to insert
     */
    void input(BigInteger element);
    
    /**
     * Queries the estimated frequency of an element
     * @param element element to query
     * @return estimated frequency
     */
    BigInteger query(BigInteger element);
    
    /**
     * Queries the top-k frequent items
     * @param k number of top items to return
     * @return map of element to frequency
     */
    Map<BigInteger, BigInteger> query(int k);
    
    /**
     * Queries all items in the sketch
     * @return map of element to frequency
     */
    Map<BigInteger, BigInteger> query();
}
