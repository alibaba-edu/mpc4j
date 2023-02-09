package edu.alibaba.mpc4j.common.tool.hash;

import edu.alibaba.mpc4j.common.tool.hash.bobhash.BobLongHash;
import edu.alibaba.mpc4j.common.tool.hash.xxhash.XxLongHash;

/**
 * 64-bit non-cryptographic hash factory.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public class LongHashFactory {
    /**
     * 64-bit non-cryptographic hash type.
     */
    public enum LongHashType {
        /**
         * BobHash64
         */
        BOB_HASH_64,
        /**
         * XXHash64
         */
        XX_HASH_64,
    }

    /**
     * Create an instance of LongHash for a given type.
     *
     * @param type the type.
     * @return an instance of LongHash.
     */
    public static LongHash createInstance(LongHashType type) {
        switch (type) {
            case BOB_HASH_64:
                return new BobLongHash();
            case XX_HASH_64:
                return new XxLongHash();
            default:
                throw new IllegalArgumentException("Invalid " + LongHashType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Create the fastest instance of LongHash.
     *
     * @return the fastest instance of LongHash.
     */
    public static LongHash fastestInstance() {
        // the efficient test shows that BobHash is the fastest one.
        return createInstance(LongHashType.BOB_HASH_64);
    }
}
