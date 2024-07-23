package edu.alibaba.mpc4j.crypto.algs.restriction;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;

/**
 * Linear bound restriction for long.
 *
 * @author Weiran Liu
 * @date 2024/5/14
 */
public class LongLinearBoundRestriction extends AbstractLongRestriction {
    /**
     * min(k)
     */
    private final double minK;
    /**
     * max(k)
     */
    private final double maxK;

    public LongLinearBoundRestriction(LongRange inputRange, LongRange outputRange, double minK, double maxK) {
        super(inputRange, outputRange);
        // the input and output range must start with 1, since 0 would lead to no sampling results
        MathPreconditions.checkEqual("0", "D.getStart()", 0, inputRange.getStart());
        MathPreconditions.checkEqual("0", "R.getStart()", 0, outputRange.getStart());
        MathPreconditions.checkNonNegative("max(k)", maxK);
        MathPreconditions.checkNonNegativeInRangeClosed("min(k)", minK, maxK);
        this.minK = minK;
        this.maxK = maxK;
    }

    @Override
    public boolean restriction(long x, long y) {
        Preconditions.checkArgument(inputRange.contains(x), "(x, y) = (%s, %s), x must be in range %s", x, y, inputRange);
        Preconditions.checkArgument(outputRange.contains(y), "(x, y) = (%s, %s), y must be in range %s", x, y, outputRange);
        return y >= Math.floor(minK * x) && y <= Math.ceil(maxK * x);
    }
}
