package edu.alibaba.mpc4j.work.db.sketch.utils.cms;

import java.math.BigInteger;

/**
 * Interface for Count-Min Sketch (CMS) implementations.
 * CMS is a probabilistic data structure for frequency estimation of items in a data stream.
 */
public interface CMS {

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
     * Queries the estimated frequency of an element
     * @param element element to query
     * @return estimated frequency
     */
    int query(BigInteger element);
    
    /**
     * Gets the internal sketch table
     * @return 2D array representing the sketch table
     */
    int[][] getTable();
}
