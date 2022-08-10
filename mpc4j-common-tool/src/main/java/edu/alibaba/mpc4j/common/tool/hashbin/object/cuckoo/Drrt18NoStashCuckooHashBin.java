package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Demmler等人提出的无贮存区布谷鸟哈希，其主要思路是放大放缩倍数ε，使得贮存区为空的概率达到要求的程度。与朴素无贮存区布谷鸟哈希的区别是，
 * DRRT18方案通过公式精确计算不同数量元素下布谷鸟哈希的桶数量。论文来源：
 * <p>
 * Demmler D, Rindal P, Rosulek M, et al. PIR-PSI: scaling private contact discovery. Proceedings on Privacy Enhancing
 * Technologies, 2018, 2018(4): 159-178.
 *
 * @author Weiran Liu
 * @date 2021/04/10
 */
class Drrt18NoStashCuckooHashBin<T> extends AbstractNoStashCuckooHashBin<T> {
    /**
     * 返回DRRT18布谷鸟哈希的桶数量。
     *
     * @param maxItemSize 期望插入的元素数量。
     * @return 桶数量。
     */
    static int getBinNum(int maxItemSize) {
        return (int) Math.ceil(maxItemSize * getEpsilon(maxItemSize));
    }

    /**
     * 返回朴素布谷鸟哈希的插入的元素数量。
     *
     * @param binNum 桶数量。
     * @return 插入的元素数量。
     */
    static int getMaxItemSize(int binNum) {
        // DRRT18的ε在变化，这里我们取最大的ε
        return (int) Math.floor(binNum / 1.58);
    }

    /**
     * 返回DRRT18布谷鸟哈希的ε取值。参数来自于PSSZ18论文，附录B: Cuckoo Hashing Failure Probability Formula的公式2。
     * 与其它布谷鸟哈希一样，这里也按照查表方式返回。
     *
     * @param maxItemSize 期望插入的元素数量。
     */
    private static double getEpsilon(int maxItemSize) {
        assert maxItemSize > 0 && maxItemSize <= CuckooHashBinFactory.MAX_ITEM_SIZE_UPPER_BOUND;
        // λ只取40。公式2为λ = a_N * e + b_N，其中a_N = 123.5，b_N = -130 - log_2(N)，其中N为期望插入的元素数量
        if (maxItemSize <= 1 << 12) {
            // (40 + 130 + 12) / 123.5 = 1.48
            return 1.48;
        } else if (maxItemSize <= 1 << 13) {
            // (40 + 130 + 13) / 123.5 = 1.49
            return 1.49;
        } else if (maxItemSize <= 1 << 14) {
            // (40 + 130 + 14) / 123.5 = 1.49
            return 1.49;
        } else if (maxItemSize <= 1 << 15) {
            // (40 + 130 + 15) / 123.5 = 1.50
            return 1.50;
        } else if (maxItemSize <= 1 << 16) {
            // (40 + 130 + 16) / 123.5 = 1.51
            return 1.51;
        } else if (maxItemSize <= 1 << 17) {
            // (40 + 130 + 17) / 123.5 = 1.52
            return 1.52;
        } else if (maxItemSize <= 1 << 18) {
            // (40 + 130 + 18) / 123.5 = 1.53
            return 1.53;
        } else if (maxItemSize <= 1 << 19) {
            // (40 + 130 + 19) / 123.5 = 1.54
            return 1.54;
        } else if (maxItemSize <= 1 << 20) {
            // (40 + 130 + 20) / 123.5 = 1.54
            return 1.54;
        } else if (maxItemSize <= 1 << 21) {
            // (40 + 130 + 21) / 123.5 = 1.55
            return 1.55;
        } else if (maxItemSize <= 1 << 22) {
            // (40 + 130 + 22) / 123.5 = 1.56
            return 1.56;
        } else if (maxItemSize <= 1 << 23) {
            // (40 + 130 + 23) / 123.5 = 1.57
            return 1.57;
        } else {
            // (40 + 130 + 24) / 123.5 = 1.58
            return 1.58;
        }
    }

    Drrt18NoStashCuckooHashBin(EnvType envType, int maxItemSize, int binNum, byte[][] keys) {
        super(envType, CuckooHashBinFactory.CuckooHashBinType.NO_STASH_DRRT18, maxItemSize, binNum, keys);
    }

    Drrt18NoStashCuckooHashBin(EnvType envType, int maxItemSize, byte[][] keys) {
        super(envType, CuckooHashBinFactory.CuckooHashBinType.NO_STASH_DRRT18, maxItemSize, keys);
    }
}
