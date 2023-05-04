package edu.alibaba.mpc4j.common.tool.galoisfield.zn64;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Zn64 Factory.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
public class Zn64Factory {
    /**
     * The Zn64 type
     */
    public enum Zn64Type {
        /**
         * Rings
         */
        RINGS,
    }

    /**
     * Creates a Zn64 instance.
     *
     * @param envType the environment.
     * @param type    the type.
     * @param n       the modulus n.
     * @return a Zn64 instance.
     */
    public static Zn64 createInstance(EnvType envType, Zn64Type type, long n) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RINGS:
                return new RingsZn64(envType, n);
            default:
                throw new IllegalArgumentException("Invalid " + Zn64Type.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a Zn64 instance.
     *
     * @param envType the environment.
     * @param n       the modulus n.
     * @return a Zn64 instance.
     */
    public static Zn64 createInstance(EnvType envType, long n) {
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return new RingsZn64(envType, n);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
