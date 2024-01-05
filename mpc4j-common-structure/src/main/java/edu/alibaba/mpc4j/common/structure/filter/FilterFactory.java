package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.security.SecureRandom;
import java.util.List;

/**
 * filter factory.
 *
 * @author Weiran Liu
 * @date 2020/06/30
 */
public class FilterFactory {
    /**
     * private constructor.
     */
    private FilterFactory() {
        // empty
    }

    /**
     * filter type.
     */
    public enum FilterType {
        /**
         * Set Filter
         */
        SET_FILTER,
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
        /**
         * Cuckoo Filter
         */
        CUCKOO_FILTER,
        /**
         * Vacuum Filter
         */
        VACUUM_FILTER,
    }

    /**
     * Gets hash key num.
     *
     * @param type filter type.
     * @return hash key num.
     */
    public static int getHashKeyNum(FilterType type) {
        switch (type) {
            case SET_FILTER:
                return SetFilter.HASH_KEY_NUM;
            case NAIVE_RANDOM_BLOOM_FILTER:
                return NaiveRandomBloomFilter.HASH_KEY_NUM;
            case SPARSE_RANDOM_BLOOM_FILTER:
                return SparseRandomBloomFilter.HASH_KEY_NUM;
            case DISTINCT_BLOOM_FILTER:
                return DistinctBloomFilter.HASH_KEY_NUM;
            case CUCKOO_FILTER:
                return CuckooFilter.HASH_KEY_NUM;
            case VACUUM_FILTER:
                return VacuumFilter.HASH_KEY_NUM;
            default:
                throw new IllegalArgumentException("Invalid " + FilterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates an empty filter.
     *
     * @param envType environment.
     * @param type    filter type.
     * @param maxSize max number of elements.
     * @param keys    keys.
     * @return an empty filter.
     */
    public static <X> Filter<X> load(EnvType envType, FilterType type, int maxSize, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, getHashKeyNum(type));
        switch (type) {
            case SET_FILTER:
                return SetFilter.create(maxSize);
            case NAIVE_RANDOM_BLOOM_FILTER:
                return NaiveRandomBloomFilter.create(envType, maxSize, keys[0]);
            case SPARSE_RANDOM_BLOOM_FILTER:
                return SparseRandomBloomFilter.create(envType, maxSize, keys[0]);
            case DISTINCT_BLOOM_FILTER:
                return DistinctBloomFilter.create(envType, maxSize, keys[0]);
            case CUCKOO_FILTER:
                return CuckooFilter.create(envType, maxSize, keys);
            case VACUUM_FILTER:
                return VacuumFilter.create(envType, maxSize, keys);
            default:
                throw new IllegalArgumentException("Invalid " + FilterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates an empty filter.
     *
     * @param envType      environment.
     * @param type         filter type.
     * @param maxSize      max number of elements.
     * @param secureRandom the random state to generate keys.
     * @return an empty filter.
     */
    public static <X> Filter<X> load(EnvType envType, FilterType type, int maxSize, SecureRandom secureRandom) {
        int hashKeyNum = getHashKeyNum(type);
        byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, secureRandom);
        return load(envType, type, maxSize, keys);
    }

    /**
     * Creates an empty merge filter.
     *
     * @param envType environment.
     * @param type    filter type.
     * @param maxSize max number of elements.
     * @param keys    keys.
     * @return an empty merge filter.
     */
    public static <X> MergeFilter<X> createMergeFilter(EnvType envType, FilterType type, int maxSize, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, getHashKeyNum(type));
        switch (type) {
            case SET_FILTER:
                return SetFilter.create(maxSize);
            case NAIVE_RANDOM_BLOOM_FILTER:
                return NaiveRandomBloomFilter.create(envType, maxSize, keys[0]);
            case SPARSE_RANDOM_BLOOM_FILTER:
                return SparseRandomBloomFilter.create(envType, maxSize, keys[0]);
            case DISTINCT_BLOOM_FILTER:
                return DistinctBloomFilter.create(envType, maxSize, keys[0]);
            default:
                throw new IllegalArgumentException("Invalid " + FilterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates the filter from {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the {@code List<byte[]>}.
     * @return the filter.
     */
    public static <X> Filter<X> load(EnvType envType, List<byte[]> byteArrayList) {
        Preconditions.checkArgument(byteArrayList.size() >= 1);
        int filterTypeOrdinal = IntUtils.byteArrayToInt(byteArrayList.get(0));
        FilterType filterType = FilterType.values()[filterTypeOrdinal];
        switch (filterType) {
            case SET_FILTER:
                return SetFilter.load(byteArrayList);
            case NAIVE_RANDOM_BLOOM_FILTER:
                return NaiveRandomBloomFilter.load(envType, byteArrayList);
            case SPARSE_RANDOM_BLOOM_FILTER:
                return SparseRandomBloomFilter.load(envType, byteArrayList);
            case DISTINCT_BLOOM_FILTER:
                return DistinctBloomFilter.load(envType, byteArrayList);
            case CUCKOO_FILTER:
                return CuckooFilter.load(envType, byteArrayList);
            case VACUUM_FILTER:
                return VacuumFilter.load(envType, byteArrayList);
            default:
                throw new IllegalArgumentException("Invalid " + FilterType.class.getSimpleName() + ": " + filterType);
        }
    }

    /**
     * Creates an empty bloom filter.
     *
     * @param envType environment.
     * @param type    filter type.
     * @param maxSize max number of elements.
     * @param key     key.
     * @return an empty bloom filter.
     */
    public static <X> BloomFilter<X> createBloomFilter(EnvType envType, FilterType type, int maxSize, byte[] key) {
        switch (type) {
            case NAIVE_RANDOM_BLOOM_FILTER:
                return NaiveRandomBloomFilter.create(envType, maxSize, key);
            case SPARSE_RANDOM_BLOOM_FILTER:
                return SparseRandomBloomFilter.create(envType, maxSize, key);
            case DISTINCT_BLOOM_FILTER:
                return DistinctBloomFilter.create(envType, maxSize, key);
            default:
                throw new IllegalArgumentException("Invalid " + FilterType.class.getSimpleName() + ": " + type);
        }
    }
}
