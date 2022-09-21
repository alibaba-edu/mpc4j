package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.security.SecureRandom;
import java.util.List;

/**
 * 过滤器工厂。
 *
 * @author Weiran Liu
 * @date 2020/06/30
 */
public class FilterFactory {
    /**
     * 私有构造函数
     */
    private FilterFactory() {
        // empty
    }

    /**
     * 过滤器类型
     */
    public enum FilterType {
        /**
         * SetFilter
         */
        SET_FILTER,
        /**
         * 布隆过滤器
         */
        BLOOM_FILTER,
        /**
         * 布谷鸟过滤器
         */
        CUCKOO_FILTER,
        /**
         * 稀疏布隆过滤器
         */
        SPARSE_BLOOM_FILTER,
        /**
         * 真空过滤器
         */
        VACUUM_FILTER,
    }

    /**
     * 返回过滤器的哈希函数数量。
     *
     * @param type    过滤器类型。
     * @param maxSize 最大元素数量。
     * @return 哈希函数数量。
     */
    public static int getHashNum(FilterType type, int maxSize) {
        switch (type) {
            case SET_FILTER:
                return SetFilter.HASH_NUM;
            case BLOOM_FILTER:
                return BloomFilter.HASH_NUM;
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
     * 创建一个过滤器。
     *
     * @param envType 环境类型。
     * @param type    过滤器类型。
     * @param maxSize 最大插入元素数量。
     * @param keys    哈希密钥。
     * @return 指定类型的过滤器。
     */
    public static <X> Filter<X> createFilter(EnvType envType, FilterType type, int maxSize, byte[][] keys) {
        assert keys.length == getHashNum(type, maxSize);
        switch (type) {
            case SET_FILTER:
                return SetFilter.create(maxSize);
            case BLOOM_FILTER:
                return BloomFilter.create(envType, maxSize, keys);
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
     * 创建一个过滤器。
     *
     * @param envType 环境类型。
     * @param maxSize 最大插入元素数量。
     * @param secureRandom 随机状态
     * @return 指定类型的过滤器。
     */
    public static <X> Filter<X> createFilter(EnvType envType, FilterType type, int maxSize, SecureRandom secureRandom) {
        int hashNum = getHashNum(type, maxSize);
        byte[][] keys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        return createFilter(envType, type, maxSize, keys);
    }

    /**
     * 创建一个可合并过滤器。
     *
     * @param envType 环境类型。
     * @param type    过滤器类型。
     * @param maxSize 期望插入的元素数量。
     * @param keys    哈希密钥。
     * @return 指定类型的可合并过滤器。
     */
    public static <X> MergeFilter<X> createMergeFilter(EnvType envType, FilterType type, int maxSize, byte[][] keys) {
        switch (type) {
            case SET_FILTER:
                return SetFilter.create(maxSize);
            case BLOOM_FILTER:
                return BloomFilter.create(envType, maxSize, keys);
            case SPARSE_BLOOM_FILTER:
                return SparseBloomFilter.create(envType, maxSize, keys);
            default:
                throw new IllegalArgumentException("Invalid " + FilterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * 创建一个过滤器。
     *
     * @param envType       环境类型。
     * @param byteArrayList 用{@code List<byte[]>}表示的过滤器。
     * @return 创建好的过滤器。
     * @throws IllegalArgumentException 如果数据包大小不正确或过滤器类型不正确。
     */
    public static <X> Filter<X> createFilter(EnvType envType, List<byte[]> byteArrayList) throws IllegalArgumentException {
        Preconditions.checkArgument(byteArrayList.size() >= 1);
        int filterTypeOrdinal = IntUtils.byteArrayToInt(byteArrayList.get(0));
        FilterType filterType = FilterType.values()[filterTypeOrdinal];
        switch (filterType) {
            case SET_FILTER:
                return SetFilter.fromByteArrayList(byteArrayList);
            case BLOOM_FILTER:
                return BloomFilter.fromByteArrayList(envType, byteArrayList);
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
