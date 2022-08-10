package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;

import java.util.ArrayList;

/**
 * 无暂存区布谷鸟哈希。
 *
 * @author Weiran Liu
 * @date 2021/04/10
 */
public interface NoStashCuckooHashBin<T> extends CuckooHashBin<T> {
    /**
     * 返回暂存区大小。
     *
     * @return 暂存区大小。
     */
    @Override
    default int stashSize() {
        return 0;
    }

    /**
     * 返回暂存区。
     *
     * @return 暂存区。
     */
    @Override
    default ArrayList<HashBinEntry<T>> getStash() {
        return new ArrayList<>(0);
    }

    /**
     * 返回桶中元素数量。
     *
     * @return 桶中元素数量。
     */
    @Override
    default int itemNumInBins() {
        return itemSize() + paddingItemSize();
    }

    /**
     * 返回暂存区中元素数量。
     *
     * @return 暂存区中元素数量。
     */
    @Override
    default int itemNumInStash() {
        return 0;
    }
}
