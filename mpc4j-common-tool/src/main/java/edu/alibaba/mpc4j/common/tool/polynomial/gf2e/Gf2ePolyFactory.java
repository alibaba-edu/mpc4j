package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * GF2E多项式插值工厂类。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
public class Gf2ePolyFactory {
    /**
     * 私有构造函数。
     */
    private Gf2ePolyFactory() {
        // empty
    }

    /**
     * GF2E多项式插值类型。
     */
    public enum Gf2ePolyType {
        /**
         * NTL库插值
         */
        NTL,
        /**
         * Rings实现的拉格朗日插值
         */
        RINGS_LAGRANGE,
        /**
         * Rings实现的牛顿插值
         */
        RINGS_NEWTON,
    }

    /**
     * 创建GF2E多项式插值实例。
     *
     * @param type 多项式插值类型。
     * @param l    有限域比特长度。
     * @return 多项式插值实例。
     */
    public static Gf2ePoly createInstance(Gf2ePolyType type, int l) {
        switch (type) {
            case NTL:
                return new NtlGf2ePoly(l);
            case RINGS_NEWTON:
                return new RingsNewtonGf2ePoly(l);
            case RINGS_LAGRANGE:
                return new RingsLagrangeGf2ePoly(l);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2ePolyType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建GF2E多项式插值实例。
     *
     * @param envType 环境类型。
     * @param l       GF2E有限域比特长度。
     * @return 多项式插值实例。
     */
    public static Gf2ePoly createInstance(EnvType envType, int l) {
        switch (envType) {
            case STANDARD:
            case INLAND:
                return createInstance(Gf2ePolyType.NTL, l);
            case STANDARD_JDK:
            case INLAND_JDK:
                return createInstance(Gf2ePolyType.RINGS_NEWTON, l);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }

    /**
     * Gets number of coefficients given n interpolated points.
     *
     * @param type type.
     * @return number of interpolated points.
     */
    public static int getCoefficientNum(Gf2ePolyType type, int num) {
        MathPreconditions.checkPositive("num", num);
        switch (type) {
            case NTL:
            case RINGS_NEWTON:
            case RINGS_LAGRANGE:
                return num;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2ePolyType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets number of coefficients given n interpolated points.
     *
     * @param envType environment.
     * @return number of interpolated points.
     */
    public static int getCoefficientNum(EnvType envType, int num) {
        switch (envType) {
            case STANDARD:
            case INLAND:
                return getCoefficientNum(Gf2ePolyType.NTL, num);
            case STANDARD_JDK:
            case INLAND_JDK:
                return getCoefficientNum(Gf2ePolyType.RINGS_NEWTON, num);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
