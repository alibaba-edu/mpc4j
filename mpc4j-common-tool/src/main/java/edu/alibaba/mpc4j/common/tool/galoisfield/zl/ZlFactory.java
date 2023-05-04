package edu.alibaba.mpc4j.common.tool.galoisfield.zl;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * The Zl factory.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
public class ZlFactory {
    /**
     * The Zl type
     */
    public enum ZlType {
        /**
         * JDK
         */
        JDK,
    }

    /**
     * Creates a Zl instance.
     *
     * @param envType the environment.
     * @param type the Zl type.
     * @param l    the l bit length.
     * @return an Zl instance.
     */
    public static Zl createInstance(EnvType envType, ZlType type, int l) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case JDK:
                return new JdkZl(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + JdkZl.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a Zl instance.
     *
     * @param l    the l bit length.
     * @return an Zl instance.
     */
    public static Zl createInstance(EnvType envType, int l) {
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return new JdkZl(envType, l);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
