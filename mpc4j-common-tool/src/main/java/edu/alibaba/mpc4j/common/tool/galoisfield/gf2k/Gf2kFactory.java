package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * GF(2^κ) factory.
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
public class Gf2kFactory {
    /**
     * private constructor.
     */
    private Gf2kFactory() {
        // empty
    }

    /**
     * GF(2^κ) type.
     */
    public enum Gf2kType {
        /**
         * combined
         */
        COMBINED,
        /**
         * NTL
         */
        NTL,
        /**
         * JDK
         */
        JDK,
        /**
         * Bouncy Castle
         */
        BC,
        /**
         * Rings
         */
        RINGS,
    }

    /**
     * Creates a GF(2^κ) instance.
     *
     * @param envType the environment.
     * @param type    type.
     * @return an instance.
     */
    public static Gf2k createInstance(EnvType envType, Gf2kType type) {
        switch (type) {
            case COMBINED:
                return new CombinedGf2k(envType);
            case NTL:
                return new NtlGf2k(envType);
            case JDK:
                return new JdkGf2k(envType);
            case BC:
                return new BcGf2k(envType);
            case RINGS:
                return new RingsGf2k(envType);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a GF(2^κ) instance.
     *
     * @param envType the environment.
     * @return an instance.
     */
    public static Gf2k createInstance(EnvType envType) {
        switch (envType) {
            case STANDARD:
                return new CombinedGf2k(envType);
            case STANDARD_JDK:
            case INLAND:
            case INLAND_JDK:
                return new BcGf2k(envType);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
