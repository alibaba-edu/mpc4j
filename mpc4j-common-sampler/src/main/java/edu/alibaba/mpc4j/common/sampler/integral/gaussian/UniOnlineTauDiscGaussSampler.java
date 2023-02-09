package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import java.util.Random;

/**
 * Uniform Online Discrete Gaussian Sampler.
 * Classical rejection sampling. Sampling from the uniform distribution and accepted with probability proportional to
 * exp(-(x - c)^2 / (2σ^2)) where exp(-(x - c)^2 / (2σ^2)) is computed in each invocation. Any real-valued c is accepted.
 * </p>
 * Modified from:
 * <p>
 * https://github.com/malb/dgs/blob/master/dgs/dgs_gauss_dp.c
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/25
 */
class UniOnlineTauDiscGaussSampler extends AbstractTauDiscGaussSampler {
    /**
     * We sample x with abs(x) <= upper_bound - 1
     */
    private final int upperBoundMinusOne;
    /**
     * There are 2 * upper_bound - 1 elements in the range [-upper_bound + 1, ..., upper_bound - 1]
     */
    private final int twoUpperBoundMinusOne;
    /**
     * Precomputed -1 / (2σ²)
     */
    private final double f;

    /**
     * Init Uniform Online Discrete Gaussian Sampler.
     *
     * @param random the random state.
     * @param c      the mean of the distribution c.
     * @param sigma  the width parameter σ.
     * @param tau    the cut-off parameter τ.
     */
    public UniOnlineTauDiscGaussSampler(Random random, int c, double sigma, int tau) {
        super(random, c, sigma, tau);
        int upperBound = DiscGaussSamplerFactory.getUpperBound(sigma, tau);
        upperBoundMinusOne = upperBound - 1;
        twoUpperBoundMinusOne = 2 * upperBound - 1;
        f = DiscGaussSamplerFactory.getUnitProbability(sigma);
    }

    @Override
    public int sample() {
        int x;
        double y, z;
        do {
            x = c + random.nextInt(twoUpperBoundMinusOne) - upperBoundMinusOne;
            z = Math.exp((x - c) * (x - c) * f);
            y = random.nextDouble();
        } while (y >= z);

        return x;
    }

    @Override
    public DiscGaussSamplerFactory.DiscGaussSamplerType getType() {
        return DiscGaussSamplerFactory.DiscGaussSamplerType.UNIFORM_ONLINE;
    }
}
