package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import edu.alibaba.mpc4j.common.sampler.integral.IntegralSampler;

/**
 * 离散高斯分布（Discrete Gaussian）采样器接口。定义参见下述论文第1节Definition 1 (Discrete Gaussian)：
 * <p>
 * Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The discrete gaussian for differential privacy. Advances in
 * Neural Information Processing Systems 33 (2020): 15676-15688.
 * <p>
 * Let µ, σ ∈ R with σ > 0. The discrete Gaussian distribution with location µ and scale σ is denoted N_Z(µ, σ^2). It
 * is a probability distribution supported on the integers.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
public interface IntegralGaussianSampler extends IntegralSampler {
    /**
     * 返回离散高斯分布的参数μ。
     *
     * @return 离散高斯分布的参数μ。
     */
    int getMu();

    /**
     * 返回离散高斯分布的参数σ。
     *
     * @return 离散高斯分布的参数σ。
     */
    double getSigma();

    @Override
    default double getMean() {
        return getMu();
    }

    @Override
    default double getVariance() {
        return getSigma() * getSigma();
    }


}
