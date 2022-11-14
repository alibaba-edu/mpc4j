package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * GF(2^128)运算工厂类。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
public class Gf2kFactory {
    /**
     * 私有构造函数
     */
    private Gf2kFactory() {
        // empty
    }

    /**
     * GF(2^128)运算类型
     */
    public enum Gf2kType {
        /**
         * 指令集实现的GF(2^128)运算
         */
        SSE,
        /**
         * 本地NTL库实现的GF(2^128)运算
         */
        NTL,
        /**
         * Bouncy Castle实现的GF(2^128)运算
         */
        BC,
        /**
         * Rings实现的GF(2^128)运算
         */
        RINGS,
    }

    /**
     * 创建GF(2^128)运算实例。
     *
     * @param type 类型。
     * @return GF(2 ^ 128)运算实例。
     */
    public static Gf2k createInstance(Gf2kType type) {
        switch (type) {
            case SSE:
                return new SseGf2k();
            case NTL:
                return new NtlGf2k();
            case BC:
                return new BcGf2k();
            case RINGS:
                return new RingsGf2k();
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 创建GF(2^128)运算实例。
     *
     * @param envType 环境类型。
     * @return GF(2 ^ 128)运算实例。
     */
    public static Gf2k createInstance(EnvType envType) {
        // 经过测试，无论何环境，都应使用BC_GF2K
        switch (envType) {
            case STANDARD:
            case STANDARD_JDK:
            case INLAND:
            case INLAND_JDK:
                return new BcGf2k();
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
