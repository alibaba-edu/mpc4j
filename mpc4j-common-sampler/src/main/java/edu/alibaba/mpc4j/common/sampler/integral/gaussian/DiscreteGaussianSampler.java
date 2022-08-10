package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.integral.geometric.DiscreteGeometricSampler;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 离散高斯分布采样，方案来自于下述论文的第5.3节，Algorithm 3: Algorithm for Sampling a Discrete Gaussian：
 * <p>
 * Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The discrete gaussian for differential privacy. Advances in
 * Neural Information Processing Systems 33 (2020): 15676-15688.
 * <p>
 * Theorem 30:
 * <p>
 * On input σ^2 ∈ Q, the procedure described in Algorithm 3 outputs one sample from N_Z(0, σ^2) and requires only a
 * constant number of operations in expection.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
public class DiscreteGaussianSampler implements IntegralGaussianSampler {
    /**
     * 随机数生成器
     */
    private final Random random;
    /**
     * 离散Laplace分布采样器
     */
    private final DiscreteGeometricSampler discreteGeometricSampler;
    /**
     * 均值μ
     */
    private final int mu;
    /**
     * 放缩系数σ
     */
    private final double sigma;
    /**
     * 辅助参数σ^2
     */
    private final double sigmaSquare;
    /**
     * 辅助参数t = ⌊σ⌋ + 1
     */
    private final int t;

    /**
     * 构建离散高斯机制采样器。
     *
     * @param mu    均值μ。
     * @param sigma 参数σ。
     */
    public DiscreteGaussianSampler(int mu, double sigma) {
        this(new SecureRandom(), mu, sigma);
    }

    /**
     * 构建离散高斯机制采样器。
     *
     * @param random 随机数生成器。
     * @param mu     均值μ。
     * @param sigma  参数σ。
     */
    public DiscreteGaussianSampler(Random random, int mu, double sigma) {
        assert sigma > 0 : "σ must be greater than 0";
        this.mu = mu;
        this.sigma = sigma;
        sigmaSquare = sigma * sigma;
        this.random = random;
        t = (int) Math.floor(sigma) + 1;
        discreteGeometricSampler = new DiscreteGeometricSampler(random, 0, t, 1);
    }

    @Override
    public int sample() {
        while (true) {
            // Sample Y ← Lap_Z(t)
            int y = discreteGeometricSampler.sample();
            // Sample C ← Bernoulli(exp(−(|Y| − σ^2/t)^2 / 2σ^2)).
            double gamma = Math.pow(Math.abs(y) - sigmaSquare / t, 2) / 2.0 / sigmaSquare;
            ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(random, gamma);
            boolean c = expBernoulliSampler.sample();
            if (c) {
                // If C = 1, return Y as output.
                return y + mu;
            }
            // If C = 0, reject and restart.
        }
    }

    @Override
    public int getMu() {
        return mu;
    }

    @Override
    public double getSigma() {
        return sigma;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        random.setSeed(seed);
    }

    @Override
    public String toString() {
        return "(μ = " + getMu() + ", σ = " + getSigma() + ")-" + getClass().getSimpleName();
    }
}
