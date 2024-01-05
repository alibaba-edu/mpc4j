package edu.alibaba.mpc4j.common.structure.okve.dokvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * blazing fast Garbled Cuckoo Table with 2 hash functions utilities.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
public class H2BlazeGctDokvsUtils {
    /**
     * private constructor
     */
    private H2BlazeGctDokvsUtils() {
        // empty
    }

    /**
     * left ε, i.e., ε_l.
     */
    private static final double LEFT_EPSILON = 2.0;

    /**
     * Gets left m. The result is shown in Lemma 5 of the paper, i.e., w = 2, e = 2.
     *
     * @param n number of key-value pairs.
     * @return lm = ε_l * n, with lm % Byte.SIZE == 0.
     */
    public static int getLm(int n) {
        MathPreconditions.checkPositive("n", n);
        return CommonUtils.getByteLength((int) Math.ceil(LEFT_EPSILON * n)) * Byte.SIZE;
    }

    /**
     * Gets right m. The result is shown in the final part of Section 3.1. That is,
     * <p>
     * α_n = λ / (g - λ - 1.9) = 7.529 / ((log_2(n) - 2.556) + 0.610).
     * </p>
     * Therefore, we can compute g with the following formula with λ = 40.
     * <p>
     * g = Math.ceil(λ * ((log_2(n) - 2.556) + 0.610) / 7.529 + 1.9) + λ.
     * </p>
     *
     * @param n number of key-value pairs.
     * @return rm = Math.ceil(λ * ((log_2(n) - 2.556) + 0.610) / 7.529 + 1.9) + λ, with rm % Byte.SIZE == 0.
     */
    public  static int getRm(int n) {
        MathPreconditions.checkPositive("n", n);
        double a = 7.529;
        double b = 0.610;
        double c = 2.556;
        // α_n = λ / g = a / (log_2(n) - c) + b
        double alphaN = a / (DoubleUtils.log2(n) - c) + b;
        // λ = α_n * g - 1.9 * α_n, g = (λ + 1.9 * α_n) / α_n = λ / α_n + 1.9, for binary row, we need to add λ
        return CommonUtils.getByteLength(
            (int) Math.ceil(CommonConstants.STATS_BIT_LENGTH / alphaN + 1.9) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
    }
}
