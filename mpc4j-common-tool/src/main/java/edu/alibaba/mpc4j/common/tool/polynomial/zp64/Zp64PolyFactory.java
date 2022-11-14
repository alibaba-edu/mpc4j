package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Zp64多项式插值工厂类。
 *
 * @author Weiran Liu
 * @date 2022/8/3
 */
public class Zp64PolyFactory {
    /**
     * 私有构造函数。
     */
    private Zp64PolyFactory() {
        // empty
    }

    /**
     * Zp64多项式插值类型。
     */
    public enum Zp64PolyType {
        /**
         * NTL实现的插值
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
     * 创建多项式插值实例。
     *
     * @param type 多项式插值类型。
     * @param l    有限域比特长度。
     * @return 多项式插值实例。
     */
    public static Zp64Poly createInstance(Zp64PolyType type, int l) {
        switch (type) {
            case NTL:
                return new NtlZp64Poly(l);
            case RINGS_NEWTON:
                return new RingsNewtonZp64Poly(l);
            case RINGS_LAGRANGE:
                return new RingsLagrangeZp64Poly(l);
            default:
                throw new IllegalArgumentException("Invalid " + Zp64PolyType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建GF2E多项式插值实例。
     *
     * @param envType 环境类型。
     * @param l       GF2E有限域比特长度。
     * @return 多项式插值实例。
     */
    public static Zp64Poly createInstance(EnvType envType, int l) {
        // 所有情况下，牛顿迭代法效率均为最优
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return createInstance(Zp64PolyType.RINGS_NEWTON, l);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }

    /**
     * 创建多项式插值实例。
     *
     * @param type 多项式插值类型。
     * @param p    质数p。
     * @return 多项式插值实例。
     */
    public static Zp64Poly createInstance(Zp64PolyType type, long p) {
        switch (type) {
            case NTL:
                return new NtlZp64Poly(p);
            case RINGS_NEWTON:
                return new RingsNewtonZp64Poly(p);
            case RINGS_LAGRANGE:
                return new RingsLagrangeZp64Poly(p);
            default:
                throw new IllegalArgumentException("Invalid " + Zp64PolyType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建GF2E多项式插值实例。
     *
     * @param envType 环境类型。
     * @param p       质数p。
     * @return 多项式插值实例。
     */
    public static Zp64Poly createInstance(EnvType envType, long p) {
        // 所有情况下，牛顿迭代法效率均为最优
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return createInstance(Zp64PolyType.RINGS_NEWTON, p);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
