package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * 浮点数计算工具。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public class BigDecimalUtils {

    /**
     * 私有构造函数。
     */
    private BigDecimalUtils() {
        // empty
    }

    /**
     * 计算给定输入的log2值。
     * 代码源自Maarten Bodewes (http://stackoverflow.com/questions/739532/logarithm-of-a-bigdecimal)
     *
     * @param val 输入值。
     * @return log2(val)。
     */
    public static double log2(BigInteger val) {
        // Get the minimum number of bits necessary to hold this value.
        int n = val.bitLength();
        // Calculate the double-precision fraction of this number; as if the
        // binary point was left of the most significant '1' bit.
        // (Get the most significant 53 bits and divide by 2^53)
        // mantissa is 53 bits (including hidden bit)
        long mask = 1L << 52;
        long mantissa = 0;
        int j = 0;
        for (int i = 1; i < 54; i++) {
            j = n - i;
            if (j < 0) {
                break;
            }

            if (val.testBit(j)) {
                mantissa |= mask;
            }
            mask >>>= 1;
        }
        // Round up if next bit is 1.
        if (j > 0 && val.testBit(j - 1)) {
            mantissa++;
        }

        double f = mantissa / (double) (1L << 52);

        // Add the logarithm to the number of bits, and subtract 1 because the
        // number of bits is always higher than necessary for a number
        // (ie. log2(val)<n for every val).
        return (n - 1 + Math.log(f) * 1.44269504088896340735992468100189213742664595415298D);
        // Magic number converts from base e to base 2 before adding. For other
        // bases, correct the result, NOT this number!
    }

    public static double log(BigDecimal val, int base) {
        return (BigIntegerUtils.log2(val.unscaledValue()) * DoubleUtils.LOG2 - val.scale() * DoubleUtils.LOG10)
            / Math.log(base);
    }

    /**
     * BigDecimal格式的0.5
     */
    public static final BigDecimal HALF = BigDecimal.valueOf(0.5);
    /**
     * 计算安全常数所对应的可忽略概率
     */
    public static final BigDecimal BLOCK_NEG_PROB = HALF.pow(CommonConstants.BLOCK_BIT_LENGTH);
    /**
     * 统计安全常数所对应的可忽略概率
     */
    public static final BigDecimal STATS_NEG_PROG = HALF.pow(CommonConstants.STATS_BIT_LENGTH);
    /**
     * BigDecimal默认精度，即保留多少位小数
     */
    public static final int PRECISION = 20;
    /**
     * BigDecimal舎入方式
     */
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
}
