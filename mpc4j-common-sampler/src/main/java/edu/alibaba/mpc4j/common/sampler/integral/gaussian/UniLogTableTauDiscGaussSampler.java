package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import edu.alibaba.mpc4j.common.sampler.binary.others.ExpfBernoulliSampler;

import java.util.Random;

/**
 * Uniform Log Table Discrete Gaussian Sampler.
 * <p>
 * Samples are drawn from a uniform distribution and accepted with probability proportional to exp(-(x - c)^2 / (2σ^2))
 * where exp(-(x - c)^2 / (2σ^2)) is computed using logarithmically many calls to Bernoulli distributions. Only
 * integer-valued c are supported.
 * </p>
 * Modified from:
 * <p>
 * https://github.com/malb/dgs/blob/master/dgs/dgs_gauss_dp.c
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/27
 */
class UniLogTableTauDiscGaussSampler extends AbstractTauDiscGaussSampler {
    /**
     * We sample x with abs(x) <= upper_bound - 1
     */
    private final int upperBoundMinusOne;
    /**
     * There are 2 * upper_bound - 1 elements in the range [-upper_bound + 1, ..., upper_bound - 1]
     */
    private final int twoUpperBoundMinusOne;
    /**
     * To realise rejection sampling, we call Bernoulli sampler with p = exp(-(x·x)/(2σ²)) and accept if it returns 1.
     */
    private final ExpfBernoulliSampler expfBernoulliSample;

    /**
     * Init Uniform Log Table Discrete Gaussian Sampler.
     *
     * @param random the random state.
     * @param c      the mean of the distribution c.
     * @param sigma  the width parameter σ.
     * @param tau    the cut-off parameter τ.
     */
    UniLogTableTauDiscGaussSampler(Random random, int c, double sigma, int tau) {
        super(random, c, sigma, tau);
        int upperBound = DiscGaussSamplerFactory.getUpperBound(sigma, tau);
        upperBoundMinusOne = upperBound - 1;
        twoUpperBoundMinusOne = 2 * upperBound - 1;
        double f = 2.0 * sigma * sigma;
        expfBernoulliSample = new ExpfBernoulliSampler(random, f, upperBound);
    }

    @Override
    public DiscGaussSamplerFactory.DiscGaussSamplerType getType() {
        return DiscGaussSamplerFactory.DiscGaussSamplerType.UNIFORM_LOG_TABLE;
    }

    @Override
    public int sample() {
        int x;
        do {
            x = random.nextInt(twoUpperBoundMinusOne) - upperBoundMinusOne;
        } while (!expfBernoulliSample.sample(x * x));
        return x + c;
    }
}
