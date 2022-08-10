package edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * PSZ18布谷鸟哈希，无暂存区。其主要思路是放大放缩倍数ε，使得贮存区为空的概率达到要求的程度。论文来源：
 * Pinkas B, Schneider T, Zohner M. Scalable private set intersection based on OT extension. ACM Transactions on
 * Privacy and Security (TOPS), 2018, 21(2): 1-35.
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
class Psz18IntNoStashCuckooHashBin extends AbstractIntNoStashCuckooHashBin {

    Psz18IntNoStashCuckooHashBin(EnvType envType, IntCuckooHashBinFactory.IntCuckooHashBinType type, int maxItemSize, byte[][] keys) {
        super(envType, type, maxItemSize, keys);
    }

    Psz18IntNoStashCuckooHashBin(EnvType envType, IntCuckooHashBinFactory.IntCuckooHashBinType type, int maxItemSize, int binNum, byte[][] keys) {
        super(envType, type, maxItemSize, binNum, keys);
    }
}
