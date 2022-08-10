package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;

/**
 * 单哈希函数的布谷鸟哈希函数。
 * <p>
 * 在非平衡PSI（Unbalanced PSI）和关键字PIR（Keyword PIR）中，如果客户端问询的集合大小是1，就需要使用单哈希函数的布谷鸟哈希。
 * </p>
 * <p>
 * 单哈希函数的布谷鸟哈希就是把1个元素插入到1个哈希桶中。但为了让接口统一，我们把单哈希函数布谷鸟哈希也作为一种特殊的布谷鸟哈希。
 * </p>
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public class OneHashCuckooHashBin<T> extends AbstractNoStashCuckooHashBin<T> {

    OneHashCuckooHashBin(EnvType envType, int binNum, byte[][] keys) {
        super(envType, CuckooHashBinType.NO_STASH_ONE_HASH, 1, binNum, keys);
    }
}
