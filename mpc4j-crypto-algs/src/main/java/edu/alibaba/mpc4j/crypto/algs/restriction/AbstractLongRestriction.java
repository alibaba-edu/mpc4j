package edu.alibaba.mpc4j.crypto.algs.restriction;

import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;

/**
 * empty long restriction.
 *
 * @author Weiran Liu
 * @date 2024/5/14
 */
abstract class AbstractLongRestriction implements LongRestriction {
    /**
     * input range
     */
    protected final LongRange inputRange;
    /**
     * output range
     */
    protected final LongRange outputRange;

    AbstractLongRestriction(LongRange inputRange, LongRange outputRange) {
        this.inputRange = inputRange;
        this.outputRange = outputRange;
    }

    @Override
    public LongRange getInputRange() {
        return inputRange;
    }

    @Override
    public LongRange getOutputRange() {
        return outputRange;
    }
}
