package edu.alibaba.mpc4j.common.sampler.real.gamma;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Marsaglia转换拒绝Gamma分布采样器。参考链接：https://en.wikipedia.org/wiki/Gamma_distribution。
 * 此实现引入了apache.commons.math3.distribution中的实现。
 *
 * This implementation uses the following algorithms:
 *
 * For 0 < shape < 1:
 * Ahrens, J. H. and Dieter, U., Computer methods for sampling from gamma, beta, Poisson and binomial distributions.
 * Computing, 12, 223-246, 1974.
 *
 * For shape >= 1:
 * Marsaglia and Tsang, A  Simple Method for Generating Gamma Variables.</i> ACM Transactions on Mathematical Software,
 * Volume 26 Issue 3, September, 2000.
 *
 * @author Weiran Liu
 * @date 2021/07/29
 */
public class ApacheGammaSampler implements GammaSampler {
    /**
     * Gamma采样器
     */
    private final GammaDistribution gammaDistribution;

    public ApacheGammaSampler(double shape, double scale) {
        this(new JDKRandomGenerator(), shape, scale);
    }

    public ApacheGammaSampler(RandomGenerator randomGenerator, double shape, double scale) {
        gammaDistribution = new GammaDistribution(randomGenerator, shape, scale);
    }

    @Override
    public double getAlpha() {
        // α = k
        return gammaDistribution.getShape();
    }

    @Override
    public double getBeta() {
        // β = 1/θ
        return 1.0 / gammaDistribution.getScale();
    }

    @Override
    public double getShape() {
        return gammaDistribution.getShape();
    }

    @Override
    public double getScale() {
        return gammaDistribution.getScale();
    }

    @Override
    public double sample() {
        return gammaDistribution.sample();
    }

    @Override
    public void reseed(long seed) {
        gammaDistribution.reseedRandomGenerator(seed);
    }

    @Override
    public String toString() {
        return "(k = " + getShape() + ", θ = " + getScale() + ")-" + getClass().getSimpleName();
    }
}
