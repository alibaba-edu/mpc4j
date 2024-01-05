package edu.alibaba.mpc4j.common.structure.okve.dokvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * naive Garbled Cuckoo Table with 2 hash functions utilities.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
public class H2NaiveGctDokvsUtils {
    /**
     * private constructor
     */
    private H2NaiveGctDokvsUtils() {
        // empty
    }

    /**
     * left ε, i.e., ε_l.
     */
    private static final double LEFT_EPSILON = 2.4;
    /**
     * right ε, i.e., ε_r.
     */
    private static final double RIGHT_EPSILON = 1.4;

    /**
     * Gets left m. The result is shown in Table 2 of the paper.
     *
     * @param n number of key-value pairs.
     * @return lm = ε_l * n, with lm % Byte.SIZE == 0.
     */
    public static int getLm(int n) {
        MathPreconditions.checkPositive("n", n);
        return CommonUtils.getByteLength((int) Math.ceil(LEFT_EPSILON * n)) * Byte.SIZE;
    }

    /**
     * Gets right m. The result is shown in the full version of the paper page 18.
     *
     * @param n number of key-value pairs.
     * @return rm = ε_r * log(n) + λ, with rm % Byte.SIZE == 0.
     */
    public static int getRm(int n) {
        MathPreconditions.checkPositive("n", n);
        return CommonUtils.getByteLength(
            (int) Math.ceil(RIGHT_EPSILON * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
    }
}
