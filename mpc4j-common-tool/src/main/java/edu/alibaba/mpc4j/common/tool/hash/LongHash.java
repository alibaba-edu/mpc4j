package edu.alibaba.mpc4j.common.tool.hash;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * Non-cryptographic hash function that outputs 64-bit integers.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public interface LongHash {
    /**
     * Gets the type of 64-bit hash.
     *
     * @return the type of 64-bit hash.
     */
    LongHashFactory.LongHashType getType();

    /**
     * Generate a 64-bit integer based on the input data.
     *
     * @param data the input data.
     * @return the generated 64-bit integer.
     */
    long hash(byte[] data);

    /**
     * Generate a 64-bit integer based on the input data and the seed.
     *
     * @param data the input data.
     * @param seed the seed.
     * @return the generated 64-bit integer.
     */
    long hash(byte[] data, long seed);

    /**
     * Generate a 64-bit integer based on the input data and the seed, then truncated to assigned byte length.
     *
     * @param data the input data.
     * @param seed the seed.
     * @param byteLength byte length.
     * @return the generated 64-bit integer.
     */
    default byte[] hash(byte[] data, long seed, int byteLength) {
        assert byteLength > 0 && byteLength <= Long.SIZE;
        long hash = hash(data, seed);
        if (byteLength != Long.SIZE) {
            hash &= (1L << byteLength * Byte.SIZE) - 1;
        }
        return LongUtils.longToFixedByteArray(hash, byteLength);
    }
}
