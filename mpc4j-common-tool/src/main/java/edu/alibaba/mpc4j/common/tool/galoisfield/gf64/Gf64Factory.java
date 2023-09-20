package edu.alibaba.mpc4j.common.tool.galoisfield.gf64;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * GF(2^64) factory.
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
public class Gf64Factory {
    /**
     * private constructor.
     */
    private Gf64Factory() {
        // empty
    }

    /**
     * GF(2^64) type.
     */
    public enum Gf64Type {
        /**
         * combined
         */
        COMBINED,
        /**
         * JDK
         */
        JDK,
        /**
         * NTL
         */
        NTL,
        /**
         * Rings
         */
        RINGS,
    }

    /**
     * Creates a GF(2^64) instance.
     *
     * @param envType the environment.
     * @param type    type.
     * @return an instance.
     */
    public static Gf64 createInstance(EnvType envType, Gf64Type type) {
        switch (type) {
            case COMBINED:
                return new CombinedGf64(envType);
            case JDK:
                return new JdkGf64(envType);
            case NTL:
                return new NtlGf64(envType);
            case RINGS:
                return new RingsGf64(envType);
            default:
                throw new IllegalArgumentException("Invalid " + Gf64Type.class.getSimpleName() + ": " + type.name());
        }
    }
}
