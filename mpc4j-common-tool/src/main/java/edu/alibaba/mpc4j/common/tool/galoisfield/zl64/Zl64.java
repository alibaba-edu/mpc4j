package edu.alibaba.mpc4j.common.tool.galoisfield.zl64;

import edu.alibaba.mpc4j.common.tool.galoisfield.LongRing;

/**
 * The Zl64 interface. All operations are done module 2^l where 0 <= l <= 62.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
public interface Zl64 extends LongRing {
    /**
     * Gets the Zl64 type.
     *
     * @return the Zl64 type.
     */
    Zl64Factory.Zl64Type getZl64Type();

    /**
     * Gets the name.
     *
     * @return the name.
     */
    @Override
    default String getName() {
        return getZl64Type().name();
    }

    /**
     * Computes a mod p.
     *
     * @param a the input a.
     * @return a mod p.
     */
    long module(final long a);
}
