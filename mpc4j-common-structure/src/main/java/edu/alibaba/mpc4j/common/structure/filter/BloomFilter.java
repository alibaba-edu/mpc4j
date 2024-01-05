package edu.alibaba.mpc4j.common.structure.filter;

/**
 * Bloom Filter interface.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public interface BloomFilter<T> extends MergeFilter<T> {
    /**
     * Gets hash indexes of the data.
     *
     * @param data data.
     * @return hash indexes of the data.
     */
    int[] hashIndexes(T data);

    /**
     * Gets the Bloom Filter storage.
     *
     * @return the Bloom Filter storage.
     */
    byte[] getStorage();

    /**
     * Gets total position num.
     *
     * @return total position num.
     */
    int getM();
}
