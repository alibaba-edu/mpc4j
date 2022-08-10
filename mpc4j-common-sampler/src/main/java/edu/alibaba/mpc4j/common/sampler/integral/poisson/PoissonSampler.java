package edu.alibaba.mpc4j.common.sampler.integral.poisson;

import edu.alibaba.mpc4j.common.sampler.integral.IntegralSampler;

/**
 * 泊松采样接口。
 *
 * 泊松分布
 * - 参数：λ
 * - 概率密度函数：f(k; λ) = Pr[X = k] = (λ^k e^(-λ)) / (k!)。
 *
 * @author Weiran Liu
 * @date 2021/07/27
 */
public interface PoissonSampler extends IntegralSampler {
    /**
     * 返回泊松采样参数λ。
     *
     * @return 泊松采样参数λ。
     */
    double getLambda();

    /**
     * 返回均值。
     *
     * @return 均值。如果未定义均值，则返回{@code Double.NaN}。
     */
    @Override
    default double getMean() {
        return getLambda();
    }

    /**
     * 返回方差。
     *
     * @return 方差（可能为{@code Double.POSITIVE_INFINITY}）。如果未定义方差，则返回{@code Double.NaN}。
     */
    @Override
    default double getVariance() {
        return getLambda();
    }
}
