package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;

/**
 * PSZ18布谷鸟哈希，无暂存区。其主要思路是放大放缩倍数ε，使得贮存区为空的概率达到要求的程度。论文来源：
 * <p>
 * Pinkas B, Schneider T, Zohner M. Scalable private set intersection based on OT extension. ACM Transactions on
 * Privacy and Security (TOPS), 2018, 21(2): 1-35.
 *
 * @author Weiran Liu
 * @date 2022/02/20
 */
class Psz18NoStashCuckooHashBin<T> extends AbstractNoStashCuckooHashBin<T> {
    /**
     * max special (small) item size
     */
    private static final int MAX_SPECIAL_ITEM_SIZE = 256;
    /**
     * 3 hashes, ε = 1.27
     */
    private static final double H3_EPSILON = 1.27;
    /**
     * 4 hashes, ε = 1.09
     */
    private static final double H4_EPSILON = 1.09;
    /**
     * 5 hashes, ε = 1.05
     */
    private static final double H5_EPSILON = 1.05;

    /**
     * Gets ε.
     *
     * @param type type.
     * @param maxItemSize max item size.
     * @return ε.
     */
    static double getEpsilon(CuckooHashBinType type, int maxItemSize) {
        MathPreconditions.checkPositiveInRangeClosed("maxItemSize", maxItemSize, CuckooHashBinFactory.MAX_ITEM_SIZE_UPPER_BOUND);
        switch (type) {
            case NO_STASH_PSZ18_3_HASH:
                // 3 hashes
                if (maxItemSize == 1) {
                    // although we can set binNum = 1 when n = 1, in some cases we must require BinNum > 1
                    return 2.0;
                } else {
                    return Math.max(NoStashCuckooHashBinUtils.getH3SmallItemSizeEpsilon(maxItemSize), H3_EPSILON);
                }
            case NO_STASH_PSZ18_4_HASH:
                // 4 hashes
                if (maxItemSize == 1) {
                    // although we can set binNum = 1 when n = 1, in some cases we must require BinNum > 1
                    return 2.0;
                } else {
                    return Math.max(NoStashCuckooHashBinUtils.getH4SmallItemSizeEpsilon(maxItemSize), H4_EPSILON);
                }
            case NO_STASH_PSZ18_5_HASH:
                // 5 hashes
                if (maxItemSize == 1) {
                    // although we can set binNum = 1 when n = 1, in some cases we must require BinNum > 1
                    return 2.0;
                } else {
                    return Math.max(NoStashCuckooHashBinUtils.getH5SmallItemSizeEpsilon(maxItemSize), H5_EPSILON);
                }
            default:
                throw new IllegalArgumentException("Invalid " + CuckooHashBinType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 返回PSZ18布谷鸟哈希的桶数量。
     *
     * @param type        布谷鸟哈希类型。
     * @param maxItemSize 预期插入的元素数量。
     * @return 桶数量。
     */
    static int getBinNum(CuckooHashBinType type, int maxItemSize) {
        return (int) Math.ceil(maxItemSize * getEpsilon(type, maxItemSize));
    }

    /**
     * 返回朴素布谷鸟哈希的插入的元素数量。
     *
     * @param type   布谷鸟哈希类型。
     * @param binNum 桶数量。
     * @return 插入的元素数量。
     */
    static int getMaxItemSize(CuckooHashBinType type, int binNum) {
        // here we do not consider special cases
        switch (type) {
            case NO_STASH_PSZ18_3_HASH:
                MathPreconditions.checkGreater("binNum", binNum, (int) Math.floor(getBinNum(type, MAX_SPECIAL_ITEM_SIZE)));
                return (int) Math.floor(binNum / H3_EPSILON);
            case NO_STASH_PSZ18_4_HASH:
                MathPreconditions.checkGreater("binNum", binNum, (int) Math.floor(getBinNum(type, MAX_SPECIAL_ITEM_SIZE)));
                return (int) Math.floor(binNum / H4_EPSILON);
            case NO_STASH_PSZ18_5_HASH:
                MathPreconditions.checkGreater("binNum", binNum, (int) Math.floor(getBinNum(type, MAX_SPECIAL_ITEM_SIZE)));
                return (int) Math.floor(binNum / H5_EPSILON);
            default:
                throw new IllegalArgumentException("Invalid " + CuckooHashBinType.class.getSimpleName() + ": " + type.name());
        }
    }

    Psz18NoStashCuckooHashBin(EnvType envType, CuckooHashBinType type, int maxItemSize, byte[][] keys) {
        super(envType, type, maxItemSize, keys);
    }

    Psz18NoStashCuckooHashBin(EnvType envType, CuckooHashBinType type, int maxItemSize, int binNum, byte[][] keys) {
        super(envType, type, maxItemSize, binNum, keys);
    }
}
