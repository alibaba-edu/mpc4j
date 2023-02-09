package edu.alibaba.mpc4j.common.tool.hash;

import edu.alibaba.mpc4j.common.tool.hash.bobhash.BobIntHash;
import edu.alibaba.mpc4j.common.tool.hash.xxhash.XxIntHash;

/**
 * 32-bit non-cryptographic hash factory.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public class IntHashFactory {
    /**
     * 32-bit non-cryptographic hash type.
     */
    public enum IntHashType {
        /**
         * BobHash32
         */
        BOB_HASH_32,
        /**
         * XXHash32
         */
        XX_HASH_32,
    }

    /**
     * Create an instance of IntHash for a given type.
     *
     * @param type the type.
     * @return an instance of IntHash.
     */
    public static IntHash createInstance(IntHashType type) {
        switch (type) {
            case BOB_HASH_32:
                return new BobIntHash();
            case XX_HASH_32:
                return new XxIntHash();
            default:
                throw new IllegalArgumentException("Invalid " + IntHashType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets the fastest IntHash type.
     *
     * @return the fastest IntHash type.
     */
    public static IntHashType fastestType() {
        // the efficient test shows that BobHash is the fastest one.
        return IntHashType.BOB_HASH_32;
    }

    /**
     * Creates the fastest instance of IntHash.
     *
     * @return the fastest instance of IntHash.
     */
    public static IntHash fastestInstance() {
        // the efficient test shows that BobHash is the fastest one.
        return createInstance(fastestType());
    }
}
