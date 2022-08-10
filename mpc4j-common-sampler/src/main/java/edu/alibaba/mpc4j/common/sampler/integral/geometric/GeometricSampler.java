package edu.alibaba.mpc4j.common.sampler.integral.geometric;

import edu.alibaba.mpc4j.common.sampler.integral.IntegralSampler;

/**
 * 双边几何分布（Two-Sided Geometric Distribution），定义参见下述论文第2.4节：
 * <p>
 * Balcer, Victor, and Salil Vadhan. Differential privacy on finite computers. arXiv preprint arXiv:1709.05396 (2017).
 * <p>
 * We say an integer-valued random variable Z follows a two-sided geometric distribution
 * with scale parameter s centered at c ∈ Z (denoted Z ∼ c + Geo(s)) if its probability mass
 * function fZ(z) is proportional to e^{−|z−c|/s}, i.e., for all z ∈ Z:
 * <p>
 * f_Z(z) = (e^{1/s} - 1) / (e^{1/s} + 1) * e^{−|z−c|/s}.
 * <p>
 * 这里使用标准定义方法，即α = e^{-1 / s}。
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
public interface GeometricSampler extends IntegralSampler {
    /**
     * 返回双边几何分布的参数μ。
     *
     * @return 双边几何分布的参数μ。
     */
    int getMu();

    /**
     * 返回双边几何分布的参数b。
     *
     * @return 双边几何分布的参数b。
     */
    double getB();

    @Override
    default double getMean() {
        return getMu();
    }

    @Override
    default double getVariance() {
        throw new UnsupportedOperationException("Do not support variance for Two-Sided Geometric Sampler");
    }
}
