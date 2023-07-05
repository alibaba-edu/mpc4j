package edu.alibaba.mpc4j.common.tool.filter;

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
         * Naive Bloom Filter
         */
        NAIVE_BLOOM_FILTER,
        /**
         * Cuckoo Filter
         */
        CUCKOO_FILTER,
        /**
         * Sparse Bloom Filter
         */
        SPARSE_BLOOM_FILTER,
        /**
         * Vacuum Filter
         */
        VACUUM_FILTER,
        /**
         * Bloom Filter without hash collision
         */
        LPRST21_BLOOM_FILTER,
    }

    /**
     * Gets hash num.
     *
     * @param type    filter type.
     * @param maxSize max number of elements.
     * @return hash num.
     */
    public static int getHashNum(FilterType type, int maxSize) {
        switch (type) {
            case SET_FILTER:
                return SetFilter.HASH_NUM;
            case NAIVE_BLOOM_FILTER:
                return NaiveBloomFilter.HASH_NUM;
            case LPRST21_BLOOM_FILTER:
                return Lprst21BloomFilter.HASH_NUM;
            case SPARSE_BLOOM_FILTER:
                return SparseBloomFilter.getHashNum(maxSize);
            case CUCKOO_FILTER:
                return CuckooFilter.HASH_NUM;
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
    public static <X> Filter<X> createFilter(EnvType envType, FilterType type, int maxSize, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, getHashNum(type, maxSize));
        switch (type) {
            case SET_FILTER:
                return SetFilter.create(maxSize);
            case NAIVE_BLOOM_FILTER:
                return NaiveBloomFilter.create(envType, maxSize, keys);
            case LPRST21_BLOOM_FILTER:
                return Lprst21BloomFilter.create(envType, maxSize, keys);
            case SPARSE_BLOOM_FILTER:
                return SparseBloomFilter.create(envType, maxSize, keys);
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
    public static <X> Filter<X> createFilter(EnvType envType, FilterType type, int maxSize, SecureRandom secureRandom) {
        int hashNum = getHashNum(type, maxSize);
        byte[][] keys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        return createFilter(envType, type, maxSize, keys);
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
        switch (type) {
            case SET_FILTER:
                return SetFilter.create(maxSize);
            case NAIVE_BLOOM_FILTER:
                return NaiveBloomFilter.create(envType, maxSize, keys);
            case LPRST21_BLOOM_FILTER:
                return Lprst21BloomFilter.create(envType, maxSize, keys);
            case SPARSE_BLOOM_FILTER:
                return SparseBloomFilter.create(envType, maxSize, keys);
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
    public static <X> Filter<X> createFilter(EnvType envType, List<byte[]> byteArrayList) {
        Preconditions.checkArgument(byteArrayList.size() >= 1);
        int filterTypeOrdinal = IntUtils.byteArrayToInt(byteArrayList.get(0));
        FilterType filterType = FilterType.values()[filterTypeOrdinal];
        switch (filterType) {
            case SET_FILTER:
                return SetFilter.fromByteArrayList(byteArrayList);
            case NAIVE_BLOOM_FILTER:
                return NaiveBloomFilter.fromByteArrayList(envType, byteArrayList);
            case LPRST21_BLOOM_FILTER:
                return Lprst21BloomFilter.fromByteArrayList(envType, byteArrayList);
            case SPARSE_BLOOM_FILTER:
                return SparseBloomFilter.fromByteArrayList(envType, byteArrayList);
            case CUCKOO_FILTER:
                return CuckooFilter.fromByteArrayList(envType, byteArrayList);
            case VACUUM_FILTER:
                return VacuumFilter.fromByteArrayList(envType, byteArrayList);
            default:
                throw new IllegalArgumentException("Invalid " + FilterType.class.getSimpleName() + ": " + filterType);
        }
    }
}
