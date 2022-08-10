package edu.alibaba.mpc4j.common.sampler;

/**
 * 采样器接口。
 *
 * @author Weiran Liu
 * @date 2021/07/30
 */
public interface Sampler {

    /**
     * 返回均值。
     *
     * @return 均值。如果未定义均值，则返回{@code Double.NaN}。
     */
    double getMean();

    /**
     * 返回方差。
     *
     * @return 方差（可能为{@code Double.POSITIVE_INFINITY}）。如果未定义方差，则返回{@code Double.NaN}。
     */
    double getVariance();

    /**
     * 重置随机数生成器的种子。如果实现的是高安全性方案，则不支持重置随机数。
     *
     * @param seed 新的种子。
     * @throws UnsupportedOperationException 如果采样算法不支持重置随机数。
     */
    void reseed(long seed) throws UnsupportedOperationException;
}
