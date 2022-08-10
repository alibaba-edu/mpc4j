package edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;

/**
 * 整数布谷鸟哈希桶工厂。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
public class IntCuckooHashBinFactory {
    /**
     * 私有构造函数。
     */
    private IntCuckooHashBinFactory() {
        // empty
    }

    /**
     * 布谷鸟哈希类型
     */
    public enum IntCuckooHashBinType {
        /**
         * 无暂存区，朴素整数布谷鸟哈希
         */
        NO_STASH_NAIVE,
        /**
         * 无暂存区，DRRT布谷鸟哈希
         */
        NO_STASH_DRRT18,
        /**
         * 无暂存区，包含3个哈希函数的PSZ18布谷鸟哈希
         */
        NO_STASH_PSZ18_3_HASH,
        /**
         * 无暂存区，包含4个哈希函数的PSZ18布谷鸟哈希
         */
        NO_STASH_PSZ18_4_HASH,
        /**
         * 无暂存区，包含5个哈希函数的PSZ18布谷鸟哈希
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
     * 构建整数布谷鸟哈希。
     *
     * @param envType     环境类型。
     * @param type        整数布谷鸟哈希类型。
     * @param maxItemSize 插入的元素数量。
     * @param keys        密钥。
     * @return 整数布谷鸟哈希。
     */
    public static IntNoStashCuckooHashBin createInstance(EnvType envType, IntCuckooHashBinType type,
                                                         int maxItemSize, byte[][] keys) {
        checkInputs(type, maxItemSize, keys);
        assert keys.length == getHashNum(type) : type.name() + " needs " + getHashNum(type) + " hash keys";
        switch (type) {
            case NO_STASH_NAIVE:
                return new NaiveIntNoStashCuckooHashBin(envType, maxItemSize, keys);
            case NO_STASH_DRRT18:
                return new Drrt18IntNoStashCuckooHashBin(envType, maxItemSize, keys);
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return new Psz18IntNoStashCuckooHashBin(envType, type, maxItemSize, keys);
            default:
                throw new IllegalArgumentException("Invalid IntCuckooHashBinType: " + type.name());
        }
    }

    /**
     * 构建整数布谷鸟哈希。
     *
     * @param envType     环境类型。
     * @param type        整数布谷鸟哈希类型。
     * @param maxItemSize 插入的元素数量。
     * @param binNum      指定哈希桶数量。
     * @param keys        密钥。
     * @return 整数布谷鸟哈希。
     */
    public static IntNoStashCuckooHashBin createInstance(EnvType envType, IntCuckooHashBinType type,
                                                         int maxItemSize, int binNum, byte[][] keys) {
        checkInputs(type, maxItemSize, binNum, keys);
        assert keys.length == getHashNum(type) : type.name() + " needs " + getHashNum(type) + " hash keys";
        switch (type) {
            case NO_STASH_NAIVE:
                return new NaiveIntNoStashCuckooHashBin(envType, maxItemSize, binNum, keys);
            case NO_STASH_DRRT18:
                return new Drrt18IntNoStashCuckooHashBin(envType, maxItemSize, binNum, keys);
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return new Psz18IntNoStashCuckooHashBin(envType, type, maxItemSize, binNum, keys);
            default:
                throw new IllegalArgumentException("Invalid IntCuckooHashBinType: " + type.name());
        }
    }

    private static void checkInputs(IntCuckooHashBinType type, int maxItemSize, byte[][] keys) {
        checkInputs(type, maxItemSize, getBinNum(type, maxItemSize), keys);
    }

    private static void checkInputs(IntCuckooHashBinType type, int maxItemSize, int binNum, byte[][] keys) {
        assert keys.length == getHashNum(type) : type.name() + " needs " + getHashNum(type) + " hash keys";
        assert maxItemSize > 0 && maxItemSize <= IntCuckooHashBinFactory.MAX_ITEM_SIZE_UPPER_BOUND
            : "maxItemSize must be in range (0, " + IntCuckooHashBinFactory.MAX_ITEM_SIZE_UPPER_BOUND + "]";
        assert binNum > maxItemSize : "binNum must be greater than maxItemSize";
    }

    /**
     * 返回与此整数布谷鸟哈希桶类型对应的对象布谷鸟哈希桶类型。
     *
     * @param intCuckooHashBinType 整数布谷鸟哈希桶类型。
     * @return 对应的对象布谷鸟哈希桶类型。
     */
    static CuckooHashBinType relateCuckooHashBinType(IntCuckooHashBinType intCuckooHashBinType) {
        switch (intCuckooHashBinType) {
            case NO_STASH_NAIVE:
                return CuckooHashBinType.NO_STASH_NAIVE;
            case NO_STASH_DRRT18:
                return CuckooHashBinType.NO_STASH_DRRT18;
            case NO_STASH_PSZ18_3_HASH:
                return CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
            case NO_STASH_PSZ18_4_HASH:
                return CuckooHashBinType.NO_STASH_PSZ18_4_HASH;
            case NO_STASH_PSZ18_5_HASH:
                return CuckooHashBinType.NO_STASH_PSZ18_5_HASH;
            default:
                throw new IllegalArgumentException("Invalid IntCuckooHashBinType: " + intCuckooHashBinType.name());
        }
    }

    /**
     * 返回整数布谷鸟哈希的哈希函数数量。
     *
     * @param type 整数布谷鸟哈希类型。
     * @return 哈希函数数量。
     */
    public static int getHashNum(IntCuckooHashBinType type) {
        CuckooHashBinType relateCuckooHashBinType = relateCuckooHashBinType(type);
        return CuckooHashBinFactory.getHashNum(relateCuckooHashBinType);
    }

    /**
     * 返回整数布谷鸟哈希的哈希桶数量。
     *
     * @param type        整数布谷鸟哈希类型。
     * @param maxItemSize 插入元素数量。
     * @return 哈希桶数量。
     */
    public static int getBinNum(IntCuckooHashBinType type, int maxItemSize) {
        CuckooHashBinType relateCuckooHashBinType = relateCuckooHashBinType(type);
        return CuckooHashBinFactory.getBinNum(relateCuckooHashBinType, maxItemSize);
    }
}
