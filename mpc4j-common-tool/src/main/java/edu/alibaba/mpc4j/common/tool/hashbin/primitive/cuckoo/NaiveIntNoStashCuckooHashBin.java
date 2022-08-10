package edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 朴素整数布谷鸟哈希，其主要思路将放缩倍数ε设置为1.5，使得贮存区为空的概率达到要求的程度。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
class NaiveIntNoStashCuckooHashBin extends AbstractIntNoStashCuckooHashBin {

    NaiveIntNoStashCuckooHashBin(EnvType envType, int maxItemSize, byte[][] keys) {
        super(envType, IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE, maxItemSize, keys);
    }

    NaiveIntNoStashCuckooHashBin(EnvType envType, int maxItemSize, int binNum, byte[][] keys) {
        super(envType, IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE, maxItemSize, binNum, keys);
    }
}
