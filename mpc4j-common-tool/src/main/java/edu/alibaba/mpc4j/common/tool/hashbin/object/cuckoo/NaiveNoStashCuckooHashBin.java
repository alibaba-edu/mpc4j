package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 朴素无贮存区布谷鸟哈希，其主要思路将放缩倍数ε设置为1.5，使得贮存区为空的概率达到要求的程度。
 * <p>
 * Angel、Chen、Laine、Setty率先在PIR中使用了此参数，论文来源：
 * <p>
 * Angel, Sebastian, Hao Chen, Kim Laine, and Srinath Setty. PIR with compressed queries and amortized query processing.
 * SP 2018, pp. 962-979. IEEE, 2018.
 * <p>
 * 由于此参数相对固定，因此后续多个论文都使用了这个方案实现无贮存区布谷鸟哈希。
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
class NaiveNoStashCuckooHashBin<T> extends AbstractNoStashCuckooHashBin<T> {
    /**
     * 朴素整数布谷鸟哈希ε取值
     */
    private static final double EPSILON = 1.5;

    /**
     * 返回布谷鸟哈希的桶数量。
     *
     * @param maxItemSize 预期插入的元素数量。
     * @return 桶数量。
     */
    static int getBinNum(int maxItemSize) {
        if (maxItemSize == 2) {
            // 2个元素时，3个哈希都映射到相同位置会死循环，概率为1 / (b^3)^2 < 1 / 2^40，则b^6 > 2^40，b = 102
            return 102;
        } else if (maxItemSize == 3) {
            // 3个元素时，3个哈希都映射到相同位置会死循环，概率为1 / (b^3)^3 < 1 / 2^40，则b^9 > 2^40，b = 22
            return 22;
        } else if (maxItemSize == 4) {
            // 4个元素时，3个哈希都映射到相同位置会死循环，概率为1 / (b^3)^4 < 1 / 2^40，则b^12 > 2^40，b = 11
            return 11;
        } else {
            // 5个元素时，3个哈希都映射到相同位置会死循环，概率为1 / (b^3)^5 < 1 / 2^40，则b^15 > 2^40，b = 7，但5个元素有8个位置
            return (int) Math.ceil(maxItemSize * EPSILON);
        }
    }

    /**
     * 返回朴素布谷鸟哈希的插入的元素数量。
     *
     * @param binNum 桶数量。
     * @return 插入的元素数量。
     */
    static int getMaxItemSize(int binNum) {
        // 给定桶大小，插入的元素数量总为binNum / ε
        return (int) Math.floor(binNum / EPSILON);
    }

    NaiveNoStashCuckooHashBin(EnvType envType, int maxItemSize, byte[][] keys) {
        super(envType, CuckooHashBinFactory.CuckooHashBinType.NO_STASH_NAIVE, maxItemSize, keys);
    }

    NaiveNoStashCuckooHashBin(EnvType envType, int maxItemSize, int binNum, byte[][] keys) {
        super(envType, CuckooHashBinFactory.CuckooHashBinType.NO_STASH_NAIVE, maxItemSize, binNum, keys);
    }
}
