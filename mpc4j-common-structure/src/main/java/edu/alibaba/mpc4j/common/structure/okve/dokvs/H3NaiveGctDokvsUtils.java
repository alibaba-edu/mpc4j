package edu.alibaba.mpc4j.common.structure.okve.dokvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * naive Garbled Cuckoo Table with 3 hash functions utilities.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
public class H3NaiveGctDokvsUtils {
    /**
     * private constructor
     */
    private H3NaiveGctDokvsUtils() {
        // empty
    }

    /**
     * left ε, i.e., ε_l.
     */
    private static final double LEFT_EPSILON = 1.3;
    /**
     * right ε, i.e., ε_r.
     */
    private static final double RIGHT_EPSILON = 0.5;

    /**
     * Gets left m. The result is shown in Section 5.4 of the paper.
     *
     * @param n number of key-value pairs.
     * @return lm = ε_l * n, with lm % Byte.SIZE == 0.
     */
    public static int getLm(int n) {
        MathPreconditions.checkPositive("n", n);
        return CommonUtils.getByteLength((int) Math.ceil(LEFT_EPSILON * n)) * Byte.SIZE;
    }

    /**
     * Gets right m. The result is shown in Section 5.4 of the paper.
     *
     * @param n number of key-value pairs.
     * @return rm = (1 + ε_r) * log(n) + λ, with with rm % Byte.SIZE == 0.
     */
    public static int getRm(int n) {
        // when n is very small, we have very high collision probabilities. Wo do the test and find that
        // n = 2^8: 186, n = 2^9: 328, n = 2^10: 561, n = 2^11: 907. When n > 2^12, use formula to compute rm.
        int r = CommonUtils.getByteLength(
            (int) Math.ceil(RIGHT_EPSILON * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
        if (n <= (1 << 8)) {
            return CommonUtils.getByteLength(Math.max(r, n)) * Byte.SIZE;
        } else if (n <= (1 << 9)) {
            // 256 < n <= 512
            return CommonUtils.getByteLength(Math.min(n, 328)) * Byte.SIZE;
        } else if (n <= (1 << 10)) {
            // 512 < n <= 1024
            return CommonUtils.getByteLength(Math.min(n, 561)) * Byte.SIZE;
        } else if (n <= (1 << 11)) {
            // 1024 < n <= 2048
            return CommonUtils.getByteLength(907) * Byte.SIZE;
        } else {
            // n > 2048
            return r;
        }
    }
}
