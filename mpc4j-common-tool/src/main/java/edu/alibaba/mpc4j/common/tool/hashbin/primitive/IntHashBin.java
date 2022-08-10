package edu.alibaba.mpc4j.common.tool.hashbin.primitive;

/**
 * 插入元素为整数的哈希桶接口。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
public interface IntHashBin {
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
     * 在哈希桶中插入元素。
     *
     * @param items 元素。
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
     * 返回哈希桶索引对应的哈希桶。
     *
     * @param binIndex 哈希桶索引。
     * @return 哈希桶索引对应的哈希桶。
     */
    int[] getBin(int binIndex);

    /**
     * 返回哈希桶索引对应哈希桶的哈希索引。
     *
     * @param binIndex 哈希桶索引。
     * @return 哈希桶索引对应哈希桶的哈希索引。
     */
    int[] getBinHashIndexes(int binIndex);

    /**
     * 返回哈希桶索引对应哈希桶的元素数量。
     *
     * @param binIndex 哈希桶索引。
     * @return 哈希桶索引对应哈希桶的元素数量。
     */
    int binSize(int binIndex);

    /**
     * 清空哈希桶
     */
    void clear();
}
