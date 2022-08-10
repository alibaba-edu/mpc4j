package edu.alibaba.mpc4j.common.sampler.real.laplace;

import edu.alibaba.mpc4j.common.sampler.real.RealSampler;

/**
 * Laplace分布采样器接口。参考链接：https://en.wikipedia.org/wiki/Laplace_distribution。
 *
 * Laplace distribution is a continuous probability distribution named after Pierre-Simon Laplace.
 * It is also sometimes called the double exponential distribution,because it can be thought of as
 * two exponential distributions (with an additional location parameter) spliced together back-to-back.
 *
 * In Laplace distribution, μ is a location parameter and b > 0,
 * which is sometimes referred to as the diversity, is a scale parameter.
 *
 * @author Weiran Liu
 * @date 2021/07/28
 */
public interface LaplaceSampler extends RealSampler {

    /**
     * 返回Laplace分布的参数μ。
     *
     * @return Laplace分布的参数μ。
     */
    double getMu();

    /**
     * 返回Laplace分布的参数b。
     *
     * @return Laplace分布的参数b。
     */
    double getB();

    @Override
    default double getMean() {
        return getMu();
    }

    @Override
    default double getVariance() {
        return 2 * Math.pow(getB(), 2);
    }
}
