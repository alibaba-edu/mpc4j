package edu.alibaba.mpc4j.common.tool.hash;

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
}
