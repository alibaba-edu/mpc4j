package edu.alibaba.mpc4j.crypto.algs.restriction;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;

/**
 * Linear Min Bound Restricted Function for long.
 *
 * @author Liqiang Peng
 * @date 2024/5/13
 */
public class LongLinearMinBoundRestriction extends AbstractLongRestriction {
    /**
     * min(k)
     */
    private final double minK;

    public LongLinearMinBoundRestriction(LongRange inputRange, LongRange outputRange, double minK) {
        super(inputRange, outputRange);
        // the input and output range must start with 0
        MathPreconditions.checkEqual("0", "D.getStart()", 0, inputRange.getStart());
        MathPreconditions.checkEqual("0", "R.getStart()", 0, outputRange.getStart());
        MathPreconditions.checkNonNegative("min(k)", minK);
        this.minK = minK;
    }

    @Override
    public boolean restriction(long x, long y) {
        Preconditions.checkArgument(inputRange.contains(x), "(x, y) = (%s, %s), x must be in range %s", x, y, inputRange);
        Preconditions.checkArgument(outputRange.contains(y), "(x, y) = (%s, %s), y must be in range %s", x, y, outputRange);
        return y >= Math.floor(x * minK);
    }
}
