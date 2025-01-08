package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.BloomFilterFactory.BloomFilterType;

/**
 * Bloom Filter. We use the description shown in Vacuum filters (defined in the following paper) for Bloom Filter.
 * <p>
 * Wang M, Zhou M, Shi S, et al. Vacuum filters: more space-efficient and faster replacement for bloom and cuckoo
 * filters[J]. Proceedings of the VLDB Endowment, 2019, 13(2): 197-210.
 * </p>
 * Bloom filters are the most well-known Approximate Membership Queries (AMQ) data structures. A Bloom filter represents
 * a set of $n$ items $S = x_1, x_2, \ldots, x_n$ by an array of $m$ bits. Each item is mapped to $k$ bits in the array
 * uses $k$ independent hash functions $h_1, h_2, \ldots, h_k$ and every mapped bit at location $h_i(x)$ is set to $1$.
 * To lookup whether an item $x_i$ is in the set, the Bloom filter checks the values in the $h_i(x)$-th bit. If all bits
 * are 1, the Bloom filter reports true. Otherwise, it reports false. A Bloom filter yields false positives. The false
 * positive rate is $ε = (1 − e^{−k · n / m})^k = (1−p)^k$.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public interface BloomFilter<T> extends Filter<T> {
    /**
     * Gets number of hash keys.
     *
     * @return number of hash keys.
     */
    static int getHashKeyNum() {
        return 1;
    }

    /**
     * Gets cuckoo filter type.
     *
     * @return cuckoo filter type.
     */
    BloomFilterType getBloomFilterType();

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

    /**
     * Merges two bloom filters.
     *
     * @param otherFilter the other bloom filter.
     */
    void merge(BloomFilter<T> otherFilter);
}
