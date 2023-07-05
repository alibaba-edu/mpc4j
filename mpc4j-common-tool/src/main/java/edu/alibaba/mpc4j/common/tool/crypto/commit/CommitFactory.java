package edu.alibaba.mpc4j.common.tool.crypto.commit;

import edu.alibaba.mpc4j.common.tool.EnvType;

import java.security.SecureRandom;

/**
 * commitment factory.
 *
 * @author Weiran Liu
 * @date 2023/3/17
 */
public class CommitFactory {
    /**
     * commitment type
     */
    public enum CommitType {
        /**
         * Random Oracle model, using SHA256 in JDK.
         */
        RO_JDK_SHA256,
        /**
         * Random Oracle model, using SM3 in Bouncy Castle.
         */
        RO_BC_SM3,
        /**
         * Random Oracle model, using SHA256 in Bouncy castle.
         */
        RO_BC_SHA256,
    }

    /**
     * Creates a commitment scheme instance.
     *
     * @param type the commitment type.
     * @return a commitment scheme instance.
     */
    public static Commit createInstance(CommitType type) {
        switch (type) {
            case RO_JDK_SHA256:
                return new RoJdkSha256Commit();
            case RO_BC_SHA256:
                return new RoBcSha256Commit();
            case RO_BC_SM3:
                return new RoBcSm3Commit();
            default:
                throw new IllegalArgumentException("Invalid " + CommitType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates a commitment scheme instance.
     *
     * @param type the commitment type.
     * @param secureRandom the random state.
     * @return a commitment scheme instance.
     */
    public static Commit createInstance(CommitType type, SecureRandom secureRandom) {
        switch (type) {
            case RO_JDK_SHA256:
                return new RoJdkSha256Commit(secureRandom);
            case RO_BC_SHA256:
                return new RoBcSha256Commit(secureRandom);
            case RO_BC_SM3:
                return new RoBcSm3Commit(secureRandom);
            default:
                throw new IllegalArgumentException("Invalid " + CommitType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates a commitment scheme instance.
     *
     * @param envType the environment.
     * @return a commitment scheme instance.
     */
    public static Commit createInstance(EnvType envType) {
        return createInstance(envType, new SecureRandom());
    }

    /**
     * Creates a commitment scheme instance.
     *
     * @param envType the environment.
     * @param secureRandom the random state.
     * @return a commitment scheme instance.
     */
    public static Commit createInstance(EnvType envType, SecureRandom secureRandom) {
        switch (envType) {
            case STANDARD:
            case STANDARD_JDK:
                return new RoBcSha256Commit(secureRandom);
            case INLAND:
            case INLAND_JDK:
                return new RoBcSm3Commit(secureRandom);
            default:
                throw new IllegalArgumentException("Invalid " + EnvType.class.getSimpleName() + ": " + envType);
        }
    }
}
