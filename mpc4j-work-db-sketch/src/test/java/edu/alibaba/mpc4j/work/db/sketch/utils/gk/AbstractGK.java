package edu.alibaba.mpc4j.work.db.sketch.utils.gk;

import java.math.BigInteger;

/**
 * Abstract base class for GK quantile sketch implementations.
 * Provides common functionality for merging and compressing representatives.
 */
public abstract class AbstractGK implements GK {
    protected long t;
    protected final float epsilon;

    /**
     * Constructs a GK sketch with specified error parameter
     *
     * @param epsilon error parameter (0 < epsilon < 1)
     */
    public AbstractGK(float epsilon) {
        this.epsilon = epsilon;
        this.t = 0;
    }

    /**
     * Tests if two representatives can be merged
     * @param e1 first representative
     * @param e2 second representative
     * @return true if mergeable, false otherwise
     */
    protected boolean testMergeable(Representative e1, Representative e2) {
        if (e1 == null || e2 == null) {
            return false;
        }
        if (e1.getT() > e2.getT() &&
                (e1.getG1().add(e1.getG2()).add(e2.getG1()).add(e2.getDelta1()).add(BigInteger.ONE)
                        .compareTo( BigInteger.valueOf((long) (epsilon * t))))<=0){
            return true;
        }
        if (e1.getT() < e2.getT() &&
                (e2.getG1().add(e2.getG2()).add(e1.getG2()).add(e1.getDelta2()).add(BigInteger.ONE)
                        .compareTo( BigInteger.valueOf((long) (epsilon * t))))<=0){
            return true;
        }
        return false;
    }

    /**
     * Merges two representatives
     * @param e1 first representative
     * @param e2 second representative
     * @return merged representative
     */
    protected Representative merging(Representative e1, Representative e2) {
        if (e1.getT() > e2.getT()) {
            e2.setG1(e1.getG1().add(e1.getG2()).add(e2.getG1()).add(BigInteger.ONE));
            return e2;
        } else {
            e1.setG2(e2.getG1().add(e2.getG2()).add(e1.getG2()).add(BigInteger.ONE));
            return e1;
        }
    }
}
