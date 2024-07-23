package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * GF(2^l) factory.
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
public class Gf2eFactory {
    /**
     * GF(2^l) type
     */
    public enum Gf2eType {
        /**
         * GF(2^l) with NTL
         */
        NTL,
        /**
         * GF(2^l) with Rings
         */
        RINGS,
        /**
         * GF(2^l) with JDK
         */
        JDK,
        /**
         * GF(2^l) with combined method
         */
        COMBINED,
    }

    /**
     * private constructor.
     */
    private Gf2eFactory() {
        // empty
    }

    /**
     * Returns if it is available for the given type and l.
     *
     * @param type type.
     * @param l    l.
     * @return true if it is available; false otherwise.
     */
    public static boolean available(Gf2eType type, int l) {
        MathPreconditions.checkPositive("l", l);
        switch (type) {
            case NTL:
            case RINGS:
                return true;
            case JDK:
                return (l == 1 || l == 2 || l == 4 || l == 8 || l == 16 | l == 32 | l == 64 || l == 128);
            case COMBINED:
                return (l == 64 || l == 128);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates a GF(2^l) instance.
     *
     * @param envType environment.
     * @param type    type.
     * @param l       l.
     * @return a GF(2^l) instance.
     */
    public static Gf2e createInstance(EnvType envType, Gf2eType type, int l) {
        switch (type) {
            case RINGS:
                return new RingsGf2e(envType, l);
            case NTL:
                return new NtlGf2e(envType, l);
            case JDK:
                if (l == 1) {
                    return new JdkGf001(envType);
                } else if (l == 2) {
                    return new JdkGf002(envType);
                } else if (l == 4) {
                    return new JdkGf004(envType);
                } else if (l == 8) {
                    return new JdkGf008(envType);
                } else if (l == 16) {
                    return new JdkGf016(envType);
                } else if (l == 32) {
                    return new JdkGf032(envType);
                } else if (l == 64) {
                    return new JdkGf064(envType);
                } else if (l == 128) {
                    return new JdkGf128(envType);
                } else {
                    throw new IllegalArgumentException("Invalid " + Gf2eType.class.getSimpleName() + " for l = " + l + ": " + type.name());
                }
            case COMBINED:
                if (l == 64) {
                    return new CombinedGf064(envType);
                } else if (l == 128) {
                    return new CombinedGf128(envType);
                } else {
                    throw new IllegalArgumentException("Invalid " + Gf2eType.class.getSimpleName() + " for l = " + l + ": " + type.name());
                }
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets type based on environment.
     *
     * @param envType environment.
     * @return type.
     */
    public static Gf2eType getType(EnvType envType, int l) {
        if (l == 1 || l == 2 || l == 4 || l == 8 || l == 16 || l == 32) {
            return Gf2eType.JDK;
        }
        if (l == 64 || l == 128) {
            switch (envType) {
                case STANDARD:
                case INLAND:
                    return Gf2eType.COMBINED;
                case STANDARD_JDK:
                case INLAND_JDK:
                    return Gf2eType.JDK;
                default:
                    throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
            }
        } else {
            switch (envType) {
                case STANDARD:
                case INLAND:
                    return Gf2eType.NTL;
                case STANDARD_JDK:
                case INLAND_JDK:
                    return Gf2eType.RINGS;
                default:
                    throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
            }
        }
    }

    /**
     * Creates a GF(2^l) instance.
     *
     * @param envType environment.
     * @param l       l.
     * @return a GF(2^l) instance.
     */
    public static Gf2e createInstance(EnvType envType, int l) {
        return createInstance(envType, getType(envType, l), l);
    }
}
