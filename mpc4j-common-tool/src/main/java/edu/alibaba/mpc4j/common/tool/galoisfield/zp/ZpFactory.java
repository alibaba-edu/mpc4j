package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import edu.alibaba.mpc4j.common.tool.EnvType;

import java.math.BigInteger;

/**
 * Zp有限域工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public class ZpFactory {
    /**
     * Zp有限域类型
     */
    public enum ZpType {
        /**
         * JDK实现的Zp运算
         */
        JDK,
    }

    /**
     * 创建Zp运算实例。
     *
     * @param envType the environment.
     * @param type 类型。
     * @param l    l比特长度。
     * @return Zp运算实例。
     */
    public static Zp createInstance(EnvType envType, ZpType type, int l) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case JDK:
                return new JdkZp(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + ZpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建Zp运算实例。
     *
     * @param envType the environment.
     * @param type  类型。
     * @param prime 素数。
     * @return Zp运算实例。
     */
    public static Zp createInstance(EnvType envType, ZpType type, BigInteger prime) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case JDK:
                return new JdkZp(envType, prime);
            default:
                throw new IllegalArgumentException("Invalid " + ZpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建Zp运算实例。
     *
     * @param envType 环境类型。
     * @param l       l比特长度。
     * @return Zp运算实例。
     */
    public static Zp createInstance(EnvType envType, int l) {
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return new JdkZp(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }

    /**
     * 创建Zp运算实例。
     *
     * @param envType 环境类型。
     * @param prime   质数。
     * @return Zp运算实例。
     */
    public static Zp createInstance(EnvType envType, BigInteger prime) {
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return new JdkZp(envType, prime);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
