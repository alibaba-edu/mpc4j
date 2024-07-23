package edu.alibaba.mpc4j.crypto.algs.restriction;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;

/**
 * Linear Max Bound Restricted Function for long.
 *
 * @author Liqiang Peng
 * @date 2024/5/13
 */
public class LongLinearMaxBoundRestriction extends AbstractLongRestriction {
    /**
     * max(k)
     */
    private final double maxK;

    public LongLinearMaxBoundRestriction(LongRange inputRange, LongRange outputRange, double maxK) {
        super(inputRange, outputRange);
        // the input and output range must start with 0
        MathPreconditions.checkEqual("0", "D.getStart()", 0, inputRange.getStart());
        MathPreconditions.checkEqual("0", "R.getStart()", 0, outputRange.getStart());
        MathPreconditions.checkNonNegative("max(k)", maxK);
        this.maxK = maxK;
    }

    @Override
    public boolean restriction(long x, long y) {
        Preconditions.checkArgument(inputRange.contains(x), "(x, y) = (%s, %s), x must be in range %s", x, y, inputRange);
        Preconditions.checkArgument(outputRange.contains(y), "(x, y) = (%s, %s), y must be in range %s", x, y, outputRange);
        return y <= Math.ceil(x * maxK);
    }
}
