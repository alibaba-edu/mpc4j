package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Zp64有限域工厂。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
public class Zp64Factory {
    /**
     * 私有构造函数
     */
    private Zp64Factory() {
        // empty
    }

    /**
     * Zp64有限域类型
     */
    public enum Zp64Type {
        /**
         * Rings实现的Zp64运算
         */
        RINGS,
    }

    /**
     * 创建Zp64运算实例。
     *
     * @param envType the environment.
     * @param type 类型。
     * @param l    l比特长度。
     * @return Zp64运算实例。
     */
    public static Zp64 createInstance(EnvType envType, Zp64Type type, int l) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RINGS:
                return new RingsZp64(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + Zp64Type.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建Zo64运算实例。
     *
     * @param envType the environment.
     * @param type  类型。
     * @param prime 素数。
     * @return Zp64运算实例。
     */
    public static Zp64 createInstance(EnvType envType, Zp64Type type, long prime) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RINGS:
                return new RingsZp64(envType, prime);
            default:
                throw new IllegalArgumentException("Invalid " + Zp64Type.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建Zp64运算实例。
     *
     * @param envType the environment.
     * @param l       l比特长度。
     * @return Zp64运算实例。
     */
    public static Zp64 createInstance(EnvType envType, int l) {
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return new RingsZp64(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }

    /**
     * 创建Zp64运算实例。
     *
     * @param envType 环境类型。
     * @param prime   质数。
     * @return Zp64运算实例。
     */
    public static Zp64 createInstance(EnvType envType, long prime) {
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return new RingsZp64(envType, prime);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
