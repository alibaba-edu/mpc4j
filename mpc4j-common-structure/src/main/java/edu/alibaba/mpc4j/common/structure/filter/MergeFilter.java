package edu.alibaba.mpc4j.common.structure.filter;

/**
 * 合并过滤器。
 *
 * @author Weiran Liu
 * @date 2021/03/30
 */
public interface MergeFilter<T> extends Filter<T> {

    /**
     * 将此过滤器与另一个过滤器合并。
     *
     * @param otherFilter 另一个过滤器。
     */
    void merge(MergeFilter<T> otherFilter);
}
