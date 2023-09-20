package edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;

/**
 * int cuckoo hash bin factory.
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
public class IntCuckooHashBinFactory {
    /**
     * private constructor.
     */
    private IntCuckooHashBinFactory() {
        // empty
    }

    /**
     * int cuckoo hash bin type
     */
    public enum IntCuckooHashBinType {
        /**
         * no-stash, naive
         */
        NO_STASH_NAIVE,
        /**
         * no-stash, 3-hash PSZ18
         */
        NO_STASH_PSZ18_3_HASH,
        /**
         * no-stash, 4-hash PSZ18
         */
        NO_STASH_PSZ18_4_HASH,
        /**
         * no-stash, 5-hash PSZ18
         */
        NO_STASH_PSZ18_5_HASH,
    }

    /**
     * max supported item size
     */
    static final int MAX_ITEM_SIZE_UPPER_BOUND = 1 << 24;
    /**
     * max total tries
     */
    static final int DEFAULT_MAX_TOTAL_TRIES = 1 << 10;

    /**
     * Creates an int cuckoo hash bin.
     *
     * @param envType     environment.
     * @param type        type.
     * @param maxItemSize max item size.
     * @param keys        keys.
     * @return an int cuckoo hash bin.
     */
    public static IntNoStashCuckooHashBin createInstance(EnvType envType, IntCuckooHashBinType type,
                                                         int maxItemSize, byte[][] keys) {
        checkInputs(type, maxItemSize, keys);
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, getHashNum(type));
        switch (type) {
            case NO_STASH_NAIVE:
                return new NaiveIntNoStashCuckooHashBin(envType, maxItemSize, keys);
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return new Psz18IntNoStashCuckooHashBin(envType, type, maxItemSize, keys);
            default:
                throw new IllegalArgumentException("Invalid " + IntCuckooHashBinType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates an int cuckoo hash bin that enforce empty stash.
     *
     * @param envType      environment.
     * @param type         type.
     * @param maxItemSize  max item size.
     * @param items        items.
     * @param secureRandom the random state to generate keys.
     * @return an int cuckoo hash bin.
     */
    public static IntNoStashCuckooHashBin createEnforceInstance(EnvType envType, IntCuckooHashBinType type,
                                                                int maxItemSize, int[] items,
                                                                SecureRandom secureRandom) {
        int hashNum = getHashNum(type);
        byte[][] keys;
        IntNoStashCuckooHashBin intNoStashCuckooHashBin = null;
        boolean success = false;
        // 重复插入，直到成功
        while (!success) {
            try {
                keys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
                intNoStashCuckooHashBin = IntCuckooHashBinFactory.createInstance(
                    envType, type, maxItemSize, keys
                );
                // R inserts α_0,...,α_{t − 1} into a Cuckoo hash table T of size m
                intNoStashCuckooHashBin.insertItems(items);
                success = true;
            } catch (ArithmeticException ignored) {

            }
        }
        Preconditions.checkNotNull(intNoStashCuckooHashBin);
        return intNoStashCuckooHashBin;
    }

    /**
     * Creates an int cuckoo hash bin.
     *
     * @param envType     environment.
     * @param type        type.
     * @param maxItemSize max item size.
     * @param binNum      number of bins.
     * @param keys        keys.
     * @return an int cuckoo hash bin.
     */
    public static IntNoStashCuckooHashBin createInstance(EnvType envType, IntCuckooHashBinType type,
                                                         int maxItemSize, int binNum, byte[][] keys) {
        checkInputs(type, maxItemSize, binNum, keys);
        switch (type) {
            case NO_STASH_NAIVE:
                return new NaiveIntNoStashCuckooHashBin(envType, maxItemSize, binNum, keys);
            case NO_STASH_PSZ18_3_HASH:
            case NO_STASH_PSZ18_4_HASH:
            case NO_STASH_PSZ18_5_HASH:
                return new Psz18IntNoStashCuckooHashBin(envType, type, maxItemSize, binNum, keys);
            default:
                throw new IllegalArgumentException("Invalid " + IntCuckooHashBinType.class.getSimpleName() + ": " + type.name());
        }
    }

    private static void checkInputs(IntCuckooHashBinType type, int maxItemSize, byte[][] keys) {
        checkInputs(type, maxItemSize, getBinNum(type, maxItemSize), keys);
    }

    private static void checkInputs(IntCuckooHashBinType type, int maxItemSize, int binNum, byte[][] keys) {
        MathPreconditions.checkEqual("hashNum", "keys.length", getHashNum(type), keys.length);
        MathPreconditions.checkPositiveInRangeClosed(
            "maxItemSize", maxItemSize, IntCuckooHashBinFactory.MAX_ITEM_SIZE_UPPER_BOUND
        );
        // the equal case is that binNum = 1 and maxItemSize = 1
        MathPreconditions.checkGreaterOrEqual("binNum", binNum, maxItemSize);
    }

    /**
     * 返回与此整数布谷鸟哈希桶类型对应的对象布谷鸟哈希桶类型。
     *
     * @param type 整数布谷鸟哈希桶类型。
     * @return 对应的对象布谷鸟哈希桶类型。
     */
    static CuckooHashBinType relateCuckooHashBinType(IntCuckooHashBinType type) {
        switch (type) {
            case NO_STASH_NAIVE:
                return CuckooHashBinType.NO_STASH_NAIVE;
            case NO_STASH_PSZ18_3_HASH:
                return CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
            case NO_STASH_PSZ18_4_HASH:
                return CuckooHashBinType.NO_STASH_PSZ18_4_HASH;
            case NO_STASH_PSZ18_5_HASH:
                return CuckooHashBinType.NO_STASH_PSZ18_5_HASH;
            default:
                throw new IllegalArgumentException("Invalid " + IntCuckooHashBinType.class.getSimpleName() + ": " + type.name());
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
