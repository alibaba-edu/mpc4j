package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;
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
     * 返回PSZ18布谷鸟哈希的桶数量。
     *
     * @param type        布谷鸟哈希类型。
     * @param maxItemSize 预期插入的元素数量。
     * @return 桶数量。
     */
    static int getBinNum(CuckooHashBinType type, int maxItemSize) {
        return (int) Math.ceil(maxItemSize * getEpsilon(type));
    }

    /**
     * 返回朴素布谷鸟哈希的插入的元素数量。
     *
     * @param type   布谷鸟哈希类型。
     * @param binNum 桶数量。
     * @return 插入的元素数量。
     */
    static int getMaxItemSize(CuckooHashBinType type, int binNum) {
        return (int) Math.floor(binNum / getEpsilon(type));
    }

    /**
     * 返回PSZ18布谷鸟哈希的ε取值。
     *
     * @param type 布谷鸟哈希类型。
     */
    private static double getEpsilon(CuckooHashBinType type) {
        switch (type) {
            case NO_STASH_PSZ18_3_HASH:
                // k = 3时，ε = 1.27
                return 1.27;
            case NO_STASH_PSZ18_4_HASH:
                // k = 4时，ε = 1.09
                return 1.09;
            case NO_STASH_PSZ18_5_HASH:
                // k = 5时，ε = 1.05
                return 1.05;
            default:
                throw new IllegalArgumentException("Invalid CuckooHashBinType:" + type.name());
        }
    }

    Psz18NoStashCuckooHashBin(EnvType envType, CuckooHashBinType type, int maxItemSize, byte[][] keys) {
        super(envType, type, maxItemSize, keys);
    }

    Psz18NoStashCuckooHashBin(EnvType envType, CuckooHashBinType type, int maxItemSize, int binNum, byte[][] keys) {
        super(envType, type, maxItemSize, binNum, keys);
    }
}
