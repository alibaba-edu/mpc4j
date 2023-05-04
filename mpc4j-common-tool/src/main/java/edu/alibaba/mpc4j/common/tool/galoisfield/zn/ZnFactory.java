package edu.alibaba.mpc4j.common.tool.galoisfield.zn;

import edu.alibaba.mpc4j.common.tool.EnvType;

import java.math.BigInteger;

/**
 * The Zn factory.
 *
 * @author Weiran Liu
 * @date 2023/3/14
 */
public class ZnFactory {
    /**
     * The Zn type
     */
    public enum ZnType {
        /**
         * JDK
         */
        JDK,
    }

    /**
     * Creates a Zn instance.
     *
     * @param envType the environment.
     * @param type    the type.
     * @param n       the modulus n.
     * @return a Zn instance.
     */
    public static Zn createInstance(EnvType envType, ZnType type, BigInteger n) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case JDK:
                return new JdkZn(envType, n);
            default:
                throw new IllegalArgumentException("Invalid " + ZnType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a Zn instance.
     *
     * @param envType the environment.
     * @param n       the modulus n.
     * @return a Zn instance.
     */
    public static Zn createInstance(EnvType envType, BigInteger n) {
        switch (envType) {
            case STANDARD:
            case INLAND:
            case STANDARD_JDK:
            case INLAND_JDK:
                return new JdkZn(envType, n);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType.name());
        }
    }
}
