package edu.alibaba.mpc4j.common.sampler.real.gaussian;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Apache Common Math3中的高斯采样器。
 *
 * @author Weiran Liu
 * @date 2021/07/29
 */
public class ApacheGaussianSampler implements GaussianSampler {
    /**
     * 高斯分布
     */
    private final NormalDistribution normalDistribution;

    public ApacheGaussianSampler(double mu, double sigma) {
        this(new JDKRandomGenerator(), mu, sigma);
    }

    public ApacheGaussianSampler(RandomGenerator randomGenerator, double mu, double sigma) {
        normalDistribution = new NormalDistribution(randomGenerator, mu, sigma);
    }

    @Override
    public double getMu() {
        return normalDistribution.getNumericalMean();
    }

    @Override
    public double getSigma() {
        return normalDistribution.getStandardDeviation();
    }

    @Override
    public double sample() {
        return normalDistribution.sample();
    }

    @Override
    public void reseed(long seed) {
        normalDistribution.reseedRandomGenerator(seed);
    }

    @Override
    public String toString() {
        return "(μ = " + getMu() + ", σ = " + getSigma() + ")-" + getClass().getSimpleName();
    }
}
