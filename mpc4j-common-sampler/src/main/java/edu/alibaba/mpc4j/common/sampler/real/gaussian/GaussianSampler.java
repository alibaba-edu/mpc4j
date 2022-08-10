package edu.alibaba.mpc4j.common.sampler.real.gaussian;

import edu.alibaba.mpc4j.common.sampler.real.RealSampler;

/**
 * 高斯分布采样器。参考链接：https://en.wikipedia.org/wiki/Normal_distribution。
 *
 * A normal (or Gaussian or Gauss or Laplace–Gauss) distribution is a type of continuous probability distribution for
 * a real-valued random variable. The parameter μ is the mean or expectation of the distribution (and also its median
 * and mode), while the parameter σ is its standard deviation.
 *
 * @author Weiran Liu
 * @date 2021/07/29
 */
public interface GaussianSampler extends RealSampler {

    /**
     * 返回高斯分布参数μ。
     *
     * @return 高斯分布参数μ。
     */
    double getMu();

    /**
     * 返回高斯分布参数σ。
     *
     * @return 高斯分布参数σ。
     */
    double getSigma();

    @Override
    default double getMean() {
        return getMu();
    }

    @Override
    default double getVariance() {
        return Math.pow(getSigma(), 2);
    }
}
