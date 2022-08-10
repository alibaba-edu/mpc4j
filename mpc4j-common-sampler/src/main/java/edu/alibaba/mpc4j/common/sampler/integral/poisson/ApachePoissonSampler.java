package edu.alibaba.mpc4j.common.sampler.integral.poisson;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Apache Common Math3中的泊松分布采样器。
 *
 * @author Weiran Liu
 * @date 2021/07/30
 */
public class ApachePoissonSampler implements PoissonSampler {
    /**
     * 泊松分布
     */
    private final PoissonDistribution poissonDistribution;

    public ApachePoissonSampler(double lambda) {
        this(new JDKRandomGenerator(), lambda);
    }

    public ApachePoissonSampler(RandomGenerator randomGenerator, double lambda) {
        assert lambda > 0 : "λ must be greater than 0";
        poissonDistribution = new PoissonDistribution(
            randomGenerator, lambda, PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS
        );
    }

    @Override
    public double getLambda() {
        return poissonDistribution.getNumericalMean();
    }

    @Override
    public int sample() {
        return poissonDistribution.sample();
    }

    @Override
    public void reseed(long seed) {
        poissonDistribution.reseedRandomGenerator(seed);
    }

    @Override
    public String toString() {
        return "(λ = " + getLambda() + ")-" + getClass().getSimpleName();
    }
}
