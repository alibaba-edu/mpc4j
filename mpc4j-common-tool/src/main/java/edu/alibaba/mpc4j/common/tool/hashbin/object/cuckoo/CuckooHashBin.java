package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 布谷鸟哈希接口。
 *
 * @author Weiran Liu
 * @date 2021/03/30
 */
public interface CuckooHashBin<T> extends HashBin<T> {
    /**
     * 返回布谷鸟哈希类型。
     *
     * @return 布谷鸟哈希类型。
     */
    CuckooHashBinType getType();

    /**
     * 返回哈希桶的元素数量。
     *
     * @return 哈希桶的元素数量。
     */
    int itemNumInBins();

    /**
     * 返回暂存区的元素数量。
     *
     * @return 暂存区的元素数量。
     */
    int itemNumInStash();

    /**
     * 返回贮存区
     *
     * @return 贮存区
     */
    ArrayList<HashBinEntry<T>> getStash();

    /**
     * 返回暂存区最大元素数量。
     *
     * @return 暂存区最大元素数量。
     */
    int stashSize();

    /**
     * 返回布谷鸟哈希桶中指定桶索引的桶中元素集合。
     *
     * @param index 哈希桶索引值。
     * @return 指定桶索引的桶中元素集合。
     */
    @Override
    default Collection<HashBinEntry<T>> getBin(int index) {
        HashBinEntry<T> hashBinEntry = getHashBinEntry(index);
        if (hashBinEntry == null) {
            return new HashSet<>(0);
        } else {
            // Cuckoo Hash中，一个桶最多只有一个元素，直接打包成集合
            Set<HashBinEntry<T>> hashBinEntrySet = new HashSet<>();
            hashBinEntrySet.add(hashBinEntry);
            return hashBinEntrySet;
        }
    }

    /**
     * 返回哈希桶索引值存储的元素。
     *
     * @param binIndex 哈希桶索引值。
     * @return 哈希桶索引值存储的元素。
     */
    HashBinEntry<T> getHashBinEntry(int binIndex);

    /**
     * 插入指定的虚拟元素。
     *
     * @param dummyItem 指定的虚拟元素。
     */
    void insertPaddingItems(T dummyItem);

    /**
     * 插入虚拟元素。
     *
     * @param secureRandom 虚拟元素。
     */
    void insertPaddingItems(SecureRandom secureRandom);

    /**
     * 返回布谷鸟哈希桶每个哈希桶支持的最大数量（固定为1）。
     *
     * @return 每个哈希桶支持的最大数量（固定为1）。
     */
    @Override
    default int maxBinSize() {
        return 1;
    }
}
