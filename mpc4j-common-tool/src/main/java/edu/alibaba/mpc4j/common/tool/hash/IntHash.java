package edu.alibaba.mpc4j.common.tool.hash;

import java.nio.ByteBuffer;

/**
 * Non-cryptographic hash function that outputs 32-bit integers.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public interface IntHash {
    /**
     * Gets the type of 32-bit hash.
     *
     * @return the type of 32-bit hash.
     */
    IntHashFactory.IntHashType getType();

    /**
     * Generate a 32-bit integer based on the input data.
     *
     * @param data the input data.
     * @return the generated 32-bit integer.
     */
    int hash(byte[] data);

    /**
     * Generate a 32-bit integer based on the input data and the seed.
     *
     * @param data the input data.
     * @param seed the seed.
     * @return the generated 32-bit integer.
     */
    int hash(byte[] data, int seed);
}
