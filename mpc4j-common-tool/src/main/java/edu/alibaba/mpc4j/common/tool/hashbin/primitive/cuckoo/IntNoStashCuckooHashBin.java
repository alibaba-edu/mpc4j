package edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo;

/**
 * 整数布谷鸟哈希桶。整数布谷鸟哈希桶一定是无暂存区布谷鸟哈希。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
public interface IntNoStashCuckooHashBin {
    /**
     * 返回整数布谷鸟哈希类型。
     *
     * @return 整数布谷鸟哈希类型。
     */
    IntCuckooHashBinFactory.IntCuckooHashBinType getType();

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
     * 插入一组元素。
     *
     * @param items 元素集合。
     * @throws IllegalArgumentException 如果插入的元素中包含重复元素。
     * @throws ArithmeticException 如果插入完元素后，哈希桶不满足安全性要求。
     */
    void insertItems(int[] items);

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
    boolean contains(int item);

    /**
     * 返回哈希桶索引值存储的元素。
     *
     * @param binIndex 哈希桶索引值。
     * @return 存储的元素。返回值小于0表示此位置无元素。
     */
    int getBinEntry(int binIndex);

    /**
     * 返回哈希桶索引值存储元素的哈希索引值。
     *
     * @param binIndex 哈希桶索引值。
     * @return 存储元素的哈希索引值。返回值小于0表示此位置无元素。
     */
    int getBinHashIndex(int binIndex);

    /**
     * 清空哈希桶
     */
    void clear();
}
