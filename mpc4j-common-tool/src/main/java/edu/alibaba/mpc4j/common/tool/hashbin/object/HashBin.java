package edu.alibaba.mpc4j.common.tool.hashbin.object;

import java.util.Collection;

/**
 * 哈希桶接口。
 *
 * @author Weiran Liu
 * @date 2021/12/20
 */
public interface HashBin<T> {
    /**
     * Gets number of hashes.
     *
     * @return number of hashes.
     */
    int getHashNum();

    /**
     * Gets hash keys.
     *
     * @return hash keys.
     */
    byte[][] getHashKeys();

    /**
     * 返回支持的最大元素数量。
     *
     * @return 支持的最大元素数量。
     */
    int maxItemSize();

    /**
     * 返回哈希桶的数量。
     *
     * @return 哈希桶的数量。
     */
    int binNum();

    /**
     * 返回每个哈希桶支持的最大数量。
     *
     * @return 每个哈希桶支持的最大数量。
     */
    int maxBinSize();

    /**
     * 在哈希桶中插入一组元素。
     *
     * @param items 元素集合。
     * @throws IllegalArgumentException 如果插入的元素中包含重复元素。
     * @throws ArithmeticException 如果插入完元素后，哈希桶不满足安全性要求。
     */
    void insertItems(Collection<T> items);

    /**
     * 返回哈希桶是否已经插入了元素。
     *
     * @return 哈希桶是否已经插入了元素。
     */
    boolean insertedItems();

    /**
     * 返回插入的元素个数。
     *
     * @return 插入的元素个数。
     */
    int itemSize();

    /**
     * 哈希桶中是否包含给定的元素。
     *
     * @param item 给定的元素。
     * @return 如果包含给定的元素，返回true；否则，返回false。
     */
    boolean contains(T item);

    /**
     * 返回给定的元素。
     *
     * @param item 给定的元素。
     * @return 返回给定的元素，如果哈希桶不包含给定的元素，则返回null。
     */
    HashBinEntry<T> get(T item);

    /**
     * 返回哈希桶索引对应的哈希桶。
     *
     * @param binIndex 哈希桶索引。
     * @return 哈希桶索引对应的哈希桶。
     */
    Collection<HashBinEntry<T>> getBin(int binIndex);

    /**
     * 返回哈希桶索引对应哈希桶的元素数量。
     *
     * @param binIndex 哈希桶索引。
     * @return 哈希桶索引对应哈希桶的元素数量。
     */
    int binSize(int binIndex);

    /**
     * 返回是否已经插入填充元素。
     *
     * @return 是否已经插入填充元素。
     */
    boolean insertedPaddingItems();

    /**
     * 返回插入填充元素的数量。
     *
     * @return 填充元素的数量。
     */
    int paddingItemSize();

    /**
     * 返回哈希桶中总元素个数。
     *
     * @return 哈希桶中总元素个数。
     */
    int size();

    /**
     * 清空哈希桶
     */
    void clear();
}
