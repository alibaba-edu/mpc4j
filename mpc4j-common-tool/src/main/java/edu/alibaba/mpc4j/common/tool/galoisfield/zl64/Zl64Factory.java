package edu.alibaba.mpc4j.common.tool.galoisfield.zl64;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * The Zl64 factory.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
public class Zl64Factory {
    /**
     * The Zl64 type
     */
    public enum Zl64Type {
        /**
         * JDK
         */
        JDK,
        /**
         * Rings
         */
        RINGS,
    }

    /**
     * Creates an instance of Zl64.
     *
     * @param envType the environment.
     * @param type    the type.
     * @param l       the l bit length.
     * @return an instance of Zl64.
     */
    public static Zl64 createInstance(EnvType envType, Zl64Type type, int l) {
        switch (type) {
            case JDK:
                return new JdkZl64(envType, l);
            case RINGS:
                return new RingsZl64(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + Zl64Type.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates an instance of Zl64.
     *
     * @param envType the environment.
     * @param l       the l bit length.
     * @return an instance of Zl64.
     */
    public static Zl64 createInstance(EnvType envType, int l) {
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return new RingsZl64(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
