package edu.alibaba.mpc4j.common.sampler.real.laplace;

import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Laplace分布采样机制。参考链接：https://en.wikipedia.org/wiki/Laplace_distribution，Computational methods。
 *
 * Given a random variable U drawn from the uniform distribution in the interval (-1/2, 1/2), the random variable
 * X = μ - b * sgn(U) * ln(1 - 2|U|)
 * has a Laplace distribution with parameters μ and b.
 * This follows from the inverse cumulative distribution function given above.
 *
 * @author Weiran Liu
 * @date 2021/07/27
 */
public class ApacheLaplaceSampler implements LaplaceSampler {
    /**
     * Laplace分布机制
     */
    private final LaplaceDistribution laplaceDistribution;

    /**
     * Laplace采样构造函数。
     *
     * @param mu 均值μ。
     * @param b  放缩系数b。
     */
    public ApacheLaplaceSampler(double mu, double b) {
        this(new JDKRandomGenerator(), mu, b);
    }

    /**
     * Laplace采样构造函数。
     *
     * @param randomGenerator 伪随机数生成器。
     * @param mu              均值μ。
     * @param b               放缩系数b。
     */
    public ApacheLaplaceSampler(RandomGenerator randomGenerator, double mu, double b) {
        laplaceDistribution = new LaplaceDistribution(randomGenerator, mu, b);
    }

    @Override
    public double sample() {
        return laplaceDistribution.sample();
    }

    @Override
    public double getMu() {
        return laplaceDistribution.getLocation();
    }

    @Override
    public double getB() {
        return laplaceDistribution.getScale();
    }

    @Override
    public void reseed(long seed) {
        laplaceDistribution.reseedRandomGenerator(seed);
    }

    @Override
    public String toString() {
        return "(μ = " + getMu() + ", b = " + getB() + ")-" + getClass().getSimpleName();
    }
}
