package edu.alibaba.mpc4j.crypto.algs.restriction;

import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;

/**
 * Restricted function for long.
 *
 * @author Liqiang Peng
 * @date 2024/5/13
 */
public interface LongRestriction {
    /**
     * Gets input range.
     *
     * @return input range.
     */
    LongRange getInputRange();

    /**
     * Gets output range.
     *
     * @return output range.
     */
    LongRange getOutputRange();

    /**
     * restricted function g(x, y).
     *
     * @param x input value.
     * @param y output value.
     * @return whether the restriction is satisfied.
     */
    boolean restriction(long x, long y);
}
