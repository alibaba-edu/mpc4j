package edu.alibaba.mpc4j.common.structure.lpn.dual.excoder;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * EX coder factory.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
public class ExCoderFactory {
    /**
     * private constructor.
     */
    private ExCoderFactory() {
        // empty
    }

    /**
     * EX coder type
     */
    public enum ExCoderType {
        /**
         * systematic EC coder, accumulator weight = 24, expander weight = 7
         */
        EX_CONV_7_24_SYSTEM,
        /**
         * systematic EC coder, accumulator weight = 24, expander weight = 21
         */
        EX_CONV_21_24_SYSTEM,
        /**
         * non-systematic EC coder, accumulator weight = 24, expander weight = 7
         */
        EX_CONV_7_24_NON_SYSTEM,
        /**
         * non-systematic EC coder, accumulator weight = 24, expander weight = 21
         */
        EX_CONV_21_24_NON_SYSTEM,
        /**
         * EA coder, accumulator weight = 1, expander weight = 7
         */
        EX_ACC_7,
        /**
         * EA coder, accumulator weight = 1, expander weight = 11
         */
        EX_ACC_11,
        /**
         * EA coder, accumulator weight = 1, expander weight = 21
         */
        EX_ACC_21,
        /**
         * EA coder, accumulator weight = 1, expander weight = 41
         */
        EX_ACC_41,
    }

    /**
     * Gets minimal distance radio.
     *
     * @param type type.
     * @return minimal distance radio.
     */
    public static double getMinDistanceRatio(ExCoderType type) {
        switch (type) {
            case EX_ACC_7:
                return 0.05;
            case EX_ACC_11:
                return 0.10;
            case EX_ACC_21:
            case EX_CONV_7_24_SYSTEM:
            case EX_CONV_7_24_NON_SYSTEM:
                return 0.15;
            case EX_ACC_41:
            case EX_CONV_21_24_SYSTEM:
            case EX_CONV_21_24_NON_SYSTEM:
                return 0.20;
            default:
                throw new IllegalArgumentException("Invalid " + ExCoderType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Gets scalar, i.e., n / k.
     *
     * @param type type.
     * @return scalar.
     */
    public static int getScalar(ExCoderType type) {
        switch (type) {
            case EX_ACC_7:
            case EX_ACC_11:
            case EX_ACC_21:
            case EX_ACC_41:
                return 5;
            case EX_CONV_7_24_SYSTEM:
            case EX_CONV_21_24_SYSTEM:
            case EX_CONV_7_24_NON_SYSTEM:
            case EX_CONV_21_24_NON_SYSTEM:
                return 2;
            default:
                throw new IllegalArgumentException("Invalid " + ExCoderType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates an EX coder.
     *
     * @param type type.
     * @param k    k.
     * @return EX coder.
     */
    public static ExCoder createExCoder(ExCoderType type, int k) {
        MathPreconditions.checkPositive("k", k);
        int scalar = getScalar(type);
        int n = scalar * k;
        switch (type) {
            case EX_ACC_7:
                return new EaCoder(k, n, 7);
            case EX_ACC_11:
                return new EaCoder(k, n, 11);
            case EX_ACC_21:
                return new EaCoder(k, n, 21);
            case EX_ACC_41:
                return new EaCoder(k, n, 41);
            case EX_CONV_7_24_SYSTEM:
                return new SystemEcCoder(k, n, 24, 7);
            case EX_CONV_21_24_SYSTEM:
                return new SystemEcCoder(k, n, 24, 21);
            case EX_CONV_7_24_NON_SYSTEM:
                return new NonSysEcCoder(k, n, 24, 7);
            case EX_CONV_21_24_NON_SYSTEM:
                return new NonSysEcCoder(k, n, 24, 21);
            default:
                throw new IllegalArgumentException("Invalid " + ExCoderType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Gets regular noise weight. With noise weight t, minimal distance ratio d, and code size n, LPN is e^{-2t d/N}
     * security against linear attacks. For regular noise, we can be slightly more accurate with (1 − 2d/N)^t. We can
     * solve t by having 2^λ = (1 − 2d/N)^t. Therefore, t = -λ / log_2(1 - 2d/N).
     *
     * @param n code size.
     * @return regular noise weight.
     */
    public static int getRegularNoiseWeight(ExCoderType type, int n) {
        MathPreconditions.checkGreater("n", n, 1);
        double minDistanceRatio = getMinDistanceRatio(type);
        // log_2(1 - 2d/N)
        double d = DoubleUtils.log2(1 - 2 * minDistanceRatio);
        // -λ / log_2(1 - 2d/N)
        double t = Math.max(CommonConstants.STATS_BIT_LENGTH, -CommonConstants.BLOCK_BIT_LENGTH / d);
        if (n < 512) {
            t = Math.max(t, 64);
        }
        // roundUpTo(t, 8);
        return CommonUtils.getByteLength((int) t) * Byte.SIZE;
    }
}
