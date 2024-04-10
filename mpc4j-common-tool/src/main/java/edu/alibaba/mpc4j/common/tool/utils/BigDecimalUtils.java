package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * BigDecimal utilities. This contains just some constants. Please use BigDecimalMath for efficient computations.
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public class BigDecimalUtils {
    /**
     * 2 in BigDecimal
     */
    public static final BigDecimal TWO = new BigDecimal(2);
    /**
     * 0.5 in BigDecimal
     */
    public static final BigDecimal HALF = BigDecimal.valueOf(0.5);
    /**
     * negligible probability for computational parameter λ, that is, 2^{-128}
     */
    public static final BigDecimal BLOCK_NEG_PROB = HALF.pow(CommonConstants.BLOCK_BIT_LENGTH);
    /**
     * negligible probability for statistical parameter σ, that is, 2^{-40}
     */
    public static final BigDecimal STATS_NEG_PROG = HALF.pow(CommonConstants.STATS_BIT_LENGTH);
    /**
     * default MathContext
     */
    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;
    /**
     * scale (precision) to ensure the result is accurate enough
     */
    public static final int PRECISION = MATH_CONTEXT.getPrecision();

    /**
     * private constructor.
     */
    private BigDecimalUtils() {
        // empty
    }

    /**
     * Computes log_b(x).
     *
     * @param x the value x.
     * @param b the base b.
     * @return log_b(x).
     */
    public static double log(BigDecimal x, int b) {
        return (BigIntegerUtils.log2(x.unscaledValue()) * DoubleUtils.LOG2 - x.scale() * DoubleUtils.LOG10) / Math.log(b);
    }
}
