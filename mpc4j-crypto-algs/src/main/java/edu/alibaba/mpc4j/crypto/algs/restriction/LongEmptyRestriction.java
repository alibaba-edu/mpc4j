package edu.alibaba.mpc4j.crypto.algs.restriction;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;

/**
 * Empty restricted function for long.
 *
 * @author Weiran Liu
 * @date 2024/5/14
 */
public class LongEmptyRestriction extends AbstractLongRestriction {

    public LongEmptyRestriction(LongRange inputRange, LongRange outputRange) {
        super(inputRange, outputRange);
    }

    @Override
    public boolean restriction(long x, long y) {
        Preconditions.checkArgument(inputRange.contains(x), "(x, y) = (%s, %s), x must be in range %s", x, y, inputRange);
        Preconditions.checkArgument(outputRange.contains(y), "(x, y) = (%s, %s), y must be in range %s", x, y, outputRange);
        return true;
    }
}
