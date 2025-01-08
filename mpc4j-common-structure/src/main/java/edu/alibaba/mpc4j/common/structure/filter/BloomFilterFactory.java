package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Bloom Filter Factory.
 *
 * @author Weiran Liu
 * @date 2024/9/19
 */
public class BloomFilterFactory {
    /**
     * private constructor.
     */
    private BloomFilterFactory() {
        // empty
    }

    /**
     * Bloom Filter type
     */
    public enum BloomFilterType {
        /**
         * Naive random Bloom Filter
         */
        NAIVE_RANDOM_BLOOM_FILTER,
        /**
         * Sparse random Bloom Filter
         */
        SPARSE_RANDOM_BLOOM_FILTER,
        /**
         * distinct Bloom Filter
         */
        DISTINCT_BLOOM_FILTER,
    }

    /**
     * Creates an empty bloom filter.
     *
     * @param envType         environment.
     * @param bloomFilterType bloom filter type.
     * @param maxSize         max number of elements.
     * @param key             key.
     * @return an empty bloom filter.
     */
    public static <X> BloomFilter<X> createBloomFilter(EnvType envType, BloomFilterType bloomFilterType,
                                                       int maxSize, byte[] key) {
        return switch (bloomFilterType) {
            case NAIVE_RANDOM_BLOOM_FILTER -> NaiveRandomBloomFilter.create(envType, maxSize, key);
            case SPARSE_RANDOM_BLOOM_FILTER -> SparseRandomBloomFilter.create(envType, maxSize, key);
            case DISTINCT_BLOOM_FILTER -> DistinctBloomFilter.create(envType, maxSize, key);
        };
    }
}
