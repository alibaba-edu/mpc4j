package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * GF2E运算工厂类。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
public class Gf2eFactory {

    /**
     * GF2E运算类型
     */
    public enum Gf2eType {
        /**
         * NTL实现的GF2E运算
         */
        NTL,
        /**
         * Rings实现的GF2E运算
         */
        RINGS,
    }

    /**
     * 私有构造函数
     */
    private Gf2eFactory() {
        // empty
    }

    /**
     * 创建GF2E运算实例。
     *
     * @param envType the environment.
     * @param type    类型。
     * @param l       l比特长度。
     * @return GF2E运算实例。
     */
    public static Gf2e createInstance(EnvType envType, Gf2eType type, int l) {
        switch (type) {
            case RINGS:
                return new RingsGf2e(envType, l);
            case NTL:
                return new NtlGf2e(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建GF2E运算实例。
     *
     * @param envType 环境类型。
     * @param l       l比特长度。
     * @return GF2E运算实例。
     */
    public static Gf2e createInstance(EnvType envType, int l) {
        switch (envType) {
            case STANDARD:
            case INLAND:
                return new NtlGf2e(envType, l);
            case STANDARD_JDK:
            case INLAND_JDK:
                return new RingsGf2e(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
