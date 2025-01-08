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
         * Naive Cuckoo Filter
         */
        NAIVE_CUCKOO_FILTER,
        /**
         * Mobile Cuckoo Filter
         */
        MOBILE_CUCKOO_FILTER,
        /**
         * Naive Vacuum Filter
         */
        NAIVE_VACUUM_FILTER,
        /**
         * Mobile Vacuum Filter
         */
        MOBILE_VACUUM_FILTER,
    }

    /**
     * Gets hash key num.
     *
     * @param type filter type.
     * @return hash key num.
     */
    public static int getHashKeyNum(FilterType type) {
        return switch (type) {
            case SET_FILTER -> SetFilter.HASH_KEY_NUM;
            case NAIVE_RANDOM_BLOOM_FILTER, SPARSE_RANDOM_BLOOM_FILTER, DISTINCT_BLOOM_FILTER -> BloomFilter.getHashKeyNum();
            case NAIVE_CUCKOO_FILTER, MOBILE_CUCKOO_FILTER, NAIVE_VACUUM_FILTER, MOBILE_VACUUM_FILTER -> CuckooFilter.getHashKeyNum();
        };
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
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, getHashKeyNum(type));
        return switch (type) {
            case SET_FILTER -> SetFilter.create(maxSize);
            case NAIVE_RANDOM_BLOOM_FILTER -> NaiveRandomBloomFilter.create(envType, maxSize, keys[0]);
            case SPARSE_RANDOM_BLOOM_FILTER -> SparseRandomBloomFilter.create(envType, maxSize, keys[0]);
            case DISTINCT_BLOOM_FILTER -> DistinctBloomFilter.create(envType, maxSize, keys[0]);
            case NAIVE_CUCKOO_FILTER -> NaiveCuckooFilter.create(envType, maxSize, keys);
            case MOBILE_CUCKOO_FILTER -> MobileCuckooFilter.create(envType, maxSize, keys);
            case NAIVE_VACUUM_FILTER -> NaiveVacuumFilter.create(envType, maxSize, keys);
            case MOBILE_VACUUM_FILTER -> MobileVacuumFilter.create(envType, maxSize, keys);
        };
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
        int hashKeyNum = getHashKeyNum(type);
        byte[][] keys = CommonUtils.generateRandomKeys(hashKeyNum, secureRandom);
        return createFilter(envType, type, maxSize, keys);
    }


    /**
     * Loads the filter from {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the {@code List<byte[]>}.
     * @return the filter.
     */
    public static <X> Filter<X> loadFilter(EnvType envType, List<byte[]> byteArrayList) {
        Preconditions.checkArgument(!byteArrayList.isEmpty());
        int filterTypeOrdinal = IntUtils.byteArrayToInt(byteArrayList.get(0));
        FilterType filterType = FilterType.values()[filterTypeOrdinal];
        return switch (filterType) {
            case SET_FILTER -> SetFilter.load(byteArrayList);
            case NAIVE_RANDOM_BLOOM_FILTER -> NaiveRandomBloomFilter.load(envType, byteArrayList);
            case SPARSE_RANDOM_BLOOM_FILTER -> SparseRandomBloomFilter.load(envType, byteArrayList);
            case DISTINCT_BLOOM_FILTER -> DistinctBloomFilter.load(envType, byteArrayList);
            case NAIVE_CUCKOO_FILTER -> NaiveCuckooFilter.load(envType, byteArrayList);
            case MOBILE_CUCKOO_FILTER -> MobileCuckooFilter.load(envType, byteArrayList);
            case NAIVE_VACUUM_FILTER -> NaiveVacuumFilter.load(envType, byteArrayList);
            case MOBILE_VACUUM_FILTER -> MobileVacuumFilter.load(envType, byteArrayList);
        };
    }
}
