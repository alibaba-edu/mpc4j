package edu.alibaba.mpc4j.common.sampler.real.gamma;

import edu.alibaba.mpc4j.common.sampler.real.RealSampler;

/**
 * Gamma分布采样器。参考链接：https://en.wikipedia.org/wiki/Gamma_distribution。
 *
 * The gamma distribution can be parameterized in terms of a shape parameter α = k
 * and an inverse scale parameter β = 1/θ, called a rate parameter.
 *
 * @author Weiran Liu
 * @date 2021/07/28
 */
public interface GammaSampler extends RealSampler {

    /**
     * 返回Gamma分布的参数α。
     *
     * @return 参数α。
     */
    double getAlpha();

    /**
     * 返回Gamma分布的参数β。
     *
     * @return 参数β。
     */
    double getBeta();

    /**
     * 返回Gamma分布的参数k。
     *
     * @return 参数k。
     */
    double getShape();

    /**
     * 返回Gamma分布的参数θ。
     *
     * @return 参数θ。
     */
    double getScale();

    @Override
    default double getMean() {
        return getShape() * getScale();
    }

    @Override
    default double getVariance() {
        return getShape() * Math.pow(getScale(), 2);
    }
}
