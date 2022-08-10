package edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Demmler等人提出的无贮存区布谷鸟哈希，其主要思路是放大放缩倍数ε，使得贮存区为空的概率达到要求的程度。与朴素无贮存区布谷鸟哈希的区别是，
 * DRRT18方案通过公式精确计算不同数量元素下布谷鸟哈希的桶数量。论文来源：
 * Demmler D, Rindal P, Rosulek M, et al. PIR-PSI: scaling private contact discovery. Proceedings on Privacy Enhancing
 * Technologies, 2018, 2018(4): 159-178.
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
class Drrt18IntNoStashCuckooHashBin extends AbstractIntNoStashCuckooHashBin {

    Drrt18IntNoStashCuckooHashBin(EnvType envType, int maxItemSize, byte[][] keys) {
        super(envType, IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_DRRT18, maxItemSize, keys);
    }

    Drrt18IntNoStashCuckooHashBin(EnvType envType, int maxItemSize, int binNum, byte[][] keys) {
        super(envType, IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_DRRT18, maxItemSize, binNum, keys);
    }
}
