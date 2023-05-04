package edu.alibaba.mpc4j.common.tool.galoisfield;

/**
 * LongField interface. Elements in LongField are represented as long.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
public interface LongField extends LongRing {
    /**
     * Computes p / q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p / q.
     */
    long div(long p, long q);

    /**
     * Computes 1 / p.
     *
     * @param p the element p.
     * @return 1 / p.
     */
    long inv(long p);
}
