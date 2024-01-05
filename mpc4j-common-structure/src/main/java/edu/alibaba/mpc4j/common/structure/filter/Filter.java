package edu.alibaba.mpc4j.common.structure.filter;

import java.util.List;

/**
 * 过滤器提供了一个近似检测某个元素是否在集合中的数据结构，且具有单边错误率：
 * - 如果过滤器判断一个元素在过滤器中，则这个元素有一定的概率实际上不在过滤器中，
 * - 如果过滤器判断一个元素不在过滤器中，则这个元素一定不在过滤器中。
 * 过滤器接口函数参考Google Guava中BloomFilter的接口进行抽象。
 *
 * Filter offers an approximate containment test with one-sided error:
 * if it claims that an element is contained in it, this might be in error,
 * but if it claims that an element is not contained in it, then this is definitely true.
 *
 * @author Weiran Liu
 * @date 2020/06/30
 */
public interface Filter<T> {
    /**
     * 返回过滤器类型。
     *
     * @return 过滤器类型。
     */
    FilterFactory.FilterType getFilterType();

    /**
     * 返回过滤器中已经插入的元素数量。
     *
     * @return 滤器中已经插入的元素数量。
     */
    int size();

    /**
     * 返回过滤器期望插入的元素数量。
     *
     * @return 过滤器期望插入的元素数量。
     */
    int maxSize();

    /**
     * 如果元素可能在此过滤器中，则返回{@code true}，如果元素一定不在过滤器中，则返回{@code false}。
     *
     * @param data 给定元素。
     * @return 如果元素可能在此过滤器中，则返回{@code true}，如果元素一定不在过滤器中，则返回{@code false}。
     */
    boolean mightContain(T data);

    /**
     * 将一个元素放置在过滤器中。
     *
     * @param data 元素。
     * @throws IllegalArgumentException 如果插入了重复的元素。
     */
    void put(T data);

    /**
     * 返回过滤器的数据压缩率。
     *
     * @return 过滤器的数据压缩率。
     */
    double ratio();

    /**
     * Packets the filter into {@code List<byte[]>}.
     *
     * @return the packet result.
     */
    List<byte[]> save();
}
