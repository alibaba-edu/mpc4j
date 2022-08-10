package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 布谷鸟哈希桶工厂。
 *
 * @author Weiran Liu
 * @date 2021/03/30
 */
public class CuckooHashBinFactory {
    /**
     * 私有构造函数
     */
    private CuckooHashBinFactory() {
        // empty
    }

    /**
     * 布谷鸟哈希类型
     */
    public enum CuckooHashBinType {
        /**
         * 包含2个哈希函数的布谷鸟哈希
         */
        NAIVE_2_HASH,
        /**
         * 包含3个哈希函数的布谷鸟哈希
         */
        NAIVE_3_HASH,
        /**
         * 包含4个哈希函数的布谷鸟哈希
         */
        NAIVE_4_HASH,
        /**
         * 包含5个哈希函数的布谷鸟哈希
         */
        NAIVE_5_HASH,
        /**
         * 单哈希函数布谷鸟哈希
         */
        NO_STASH_ONE_HASH,
        /**
         * 无暂存区布谷鸟哈希
         */
        NO_STASH_DRRT18,
        /**
         * 无暂存区，包含3个哈希函数的YWL20布谷鸟哈希
         */
        NO_STASH_NAIVE,
        /**
         * 无暂存区，包含3个哈希函数的PSZ18布谷鸟哈希
         */
        NO_STASH_PSZ18_3_HASH,
        /**
         * 无暂存区，包含4个哈希函数的PSZ18布谷鸟哈希
         */
        NO_STASH_PSZ18_4_HASH,
        /**
         * 无暂存区，包含3个哈希函数的PSZ18布谷鸟哈希
         */
        NO_STASH_PSZ18_5_HASH,
    }

    /**
     * 布谷鸟哈希支持插的最大元素数量
     */
    static final int MAX_ITEM_SIZE_UPPER_BOUND = 1 << 24;
    /**
     * 驱逐元素的最大尝试次数
     */
    static final int DEFAULT_MAX_TOTAL_TRIES = 1 << 10;

    /**
     * 构建布谷鸟哈希。
     *
     * @param envType     环境类型。
     * @param type        布谷鸟哈希类型。
     * @param maxItemSize 插入的元素数量。
     * @param keys        密钥。
     * @param <T>         布谷鸟哈希中存储元素的类型。
     * @return 布谷鸟哈希。
     */
    public static <T> CuckooHashBin<T> createCuckooHashBin(EnvType envType, CuckooHashBinType type,
                                                           int maxItemSize, byte[][] keys) {
        checkInputs(type, maxItemSize, keys);
        // 单哈希布谷鸟哈希必须要指定桶大小，因此不允许通过此函数构建单哈希布谷鸟哈希。
        switch (type) {
            case NAIVE_2_HASH:
            case NAIVE_3_HASH:
            case NAIVE_4_HASH:
            case NAIVE_5_HASH:
                return new NaiveCuckooHashBin<>(envType, type, maxItemSize, keys);
            case NO_STASH_NAIVE:
                return new NaiveNoStashCuckooHashBin<>(envType, maxItemSize, keys);
            case NO_STASH_DRRT18:
                return new Drrt18NoStashCuckooHashBin<>(envType, maxItemSize, keys);
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return new Psz18NoStashCuckooHashBin<>(envType, type, maxItemSize, keys);
            default:
                throw new IllegalArgumentException("Invalid CuckooHashBinType: " + type.name());
        }
    }

    /**
     * 构建布谷鸟哈希。
     *
     * @param envType     环境类型。
     * @param type        布谷鸟哈希类型。
     * @param maxItemSize 插入的元素数量。
     * @param binNum      指定桶数量。
     * @param keys        密钥。
     * @param <T>         布谷鸟哈希中存储元素的类型。
     * @return 布谷鸟哈希。
     */
    public static <T> CuckooHashBin<T> createCuckooHashBin(EnvType envType, CuckooHashBinType type,
                                                           int maxItemSize, int binNum, byte[][] keys) {
        checkInputs(type, maxItemSize, binNum, keys);
        switch (type) {
            case NO_STASH_ONE_HASH:
                return new OneHashCuckooHashBin<>(envType, binNum, keys);
            case NAIVE_2_HASH:
            case NAIVE_3_HASH:
            case NAIVE_4_HASH:
            case NAIVE_5_HASH:
                return new NaiveCuckooHashBin<>(envType, type, maxItemSize, binNum, keys);
            case NO_STASH_NAIVE:
                return new NaiveNoStashCuckooHashBin<>(envType, maxItemSize, binNum, keys);
            case NO_STASH_DRRT18:
                return new Drrt18NoStashCuckooHashBin<>(envType, maxItemSize, binNum, keys);
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return new Psz18NoStashCuckooHashBin<>(envType, type, maxItemSize, binNum, keys);
            default:
                throw new IllegalArgumentException("Invalid CuckooHashBinType: " + type.name());
        }
    }

    private static void checkInputs(CuckooHashBinType type, int maxItemSize, byte[][] keys) {
        checkInputs(type, maxItemSize, getBinNum(type, maxItemSize), keys);
    }

    private static void checkInputs(CuckooHashBinType type, int maxItemSize, int binNum, byte[][] keys) {
        assert keys.length == getHashNum(type) : type.name() + " needs " + getHashNum(type) + " hash keys";
        switch (type) {
            case NO_STASH_ONE_HASH:
                assert maxItemSize == 1 : type.name() + " only support inserting exactly one item: " + maxItemSize;
                break;
            case NAIVE_2_HASH:
            case NAIVE_3_HASH:
            case NAIVE_4_HASH:
            case NAIVE_5_HASH:
            case NO_STASH_NAIVE:
            case NO_STASH_DRRT18:
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                assert maxItemSize > 0 && maxItemSize <= CuckooHashBinFactory.MAX_ITEM_SIZE_UPPER_BOUND
                    : "maxItemSize must be in range (0, " + CuckooHashBinFactory.MAX_ITEM_SIZE_UPPER_BOUND + "]";
                assert binNum > maxItemSize : "binNum must be greater than maxItemSize";
                break;
            default:
                throw new IllegalArgumentException("Invalid CuckooHashBinType: " + type.name());
        }
    }

    /**
     * 返回布谷鸟哈希的哈希函数数量。
     *
     * @param type 布谷鸟哈希类型。
     * @return 哈希函数数量。
     */
    public static int getHashNum(CuckooHashBinType type) {
        switch (type) {
            case NO_STASH_ONE_HASH:
                return 1;
            case NAIVE_2_HASH:
                return 2;
            case NAIVE_3_HASH:
            case NO_STASH_NAIVE:
            case NO_STASH_DRRT18:
            case NO_STASH_PSZ18_3_HASH:
                return 3;
            case NAIVE_4_HASH:
            case NO_STASH_PSZ18_4_HASH:
                return 4;
            case NAIVE_5_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return 5;
            default:
                throw new IllegalArgumentException("Invalid CuckooHashBinType: " + type.name());
        }
    }

    /**
     * 返回布谷鸟哈希的哈希桶数量。
     *
     * @param type        布谷鸟哈希类型。
     * @param maxItemSize 插入元素数量。
     * @return 哈希桶数量。
     */
    public static int getBinNum(CuckooHashBinType type, int maxItemSize) {
        // 单哈希布谷鸟哈希必须要指定桶大小，因此不允许通过此函数得到单哈希函数布谷鸟哈希的桶数量
        switch (type) {
            case NAIVE_2_HASH:
            case NAIVE_3_HASH:
            case NAIVE_4_HASH:
            case NAIVE_5_HASH:
                return NaiveCuckooHashBin.getBinNum(type, maxItemSize);
            case NO_STASH_NAIVE:
                return NaiveNoStashCuckooHashBin.getBinNum(maxItemSize);
            case NO_STASH_DRRT18:
                return Drrt18NoStashCuckooHashBin.getBinNum(maxItemSize);
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return Psz18NoStashCuckooHashBin.getBinNum(type, maxItemSize);
            default:
                throw new IllegalArgumentException("Invalid CuckooHashBinType: " + type.name());
        }
    }

    /**
     * 返回布谷鸟哈希插入元素的最大数量。
     *
     * @param type        布谷鸟哈希类型。
     * @param binNum      哈希桶数量。
     * @return 插入元素的最大数量。
     */
    public static int getMaxItemSize(CuckooHashBinType type, int binNum) {
        switch (type) {
            case NO_STASH_ONE_HASH:
                return 1;
            case NAIVE_2_HASH:
            case NAIVE_3_HASH:
            case NAIVE_4_HASH:
            case NAIVE_5_HASH:
                return NaiveCuckooHashBin.getMaxItemSize(type, binNum);
            case NO_STASH_NAIVE:
                return NaiveNoStashCuckooHashBin.getMaxItemSize(binNum);
            case NO_STASH_DRRT18:
                return Drrt18NoStashCuckooHashBin.getMaxItemSize(binNum);
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return Psz18NoStashCuckooHashBin.getBinNum(type, binNum);
            default:
                throw new IllegalArgumentException("Invalid CuckooHashBinType: " + type.name());
        }
    }

    /**
     * 返回布谷鸟哈希的暂存区大小。
     *
     * @param type        布谷鸟哈希类型。
     * @param maxItemSize 插入的元素数量。
     * @return 暂存区大小。
     */
    public static int getStashSize(CuckooHashBinType type, int maxItemSize) {
        switch (type) {
            case NAIVE_2_HASH:
            case NAIVE_3_HASH:
            case NAIVE_4_HASH:
            case NAIVE_5_HASH:
                return NaiveCuckooHashBin.getStashSize(maxItemSize);
            case NO_STASH_NAIVE:
            case NO_STASH_DRRT18:
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return 0;
            default:
                throw new IllegalArgumentException("Invalid CuckooHashBinType: " + type.name());
        }
    }
}
