package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * Uniform Table Discrete Gaussian Sampler.
 * <p>
 * Classical rejection sampling. Sampling from the uniform distribution and accepted with probability proportional to
 * exp(-(x - c)^2 / (2σ^2)) where exp(-(x - c)^2 / (2σ^2)) is precomputed and stored in a table. Any real-valued c is
 * accepted.
 * </p>
 * Modified from:
 * <p>
 * https://github.com/malb/dgs/blob/master/dgs/dgs_gauss_dp.c
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/25
 */
class UniTableTauDiscGaussSampler extends AbstractTauDiscGaussSampler {
    /**
     * We sample x with abs(x) < upper_bound
     */
    private final int upperBound;
    /**
     * Precomputed -1 / (2σ²)
     */
    private final double f;
    /**
     * Precomputed values for exp(-(x - 2)² / (2σ²))
     */
    private final double[] rho;

    /**
     * Init Uniform Table Discrete Gaussian Sampler.
     *
     * @param random the random state.
     * @param c      the mean of the distribution c.
     * @param sigma  the width parameter σ.
     * @param tau    the cut-off parameter τ.
     */
    public UniTableTauDiscGaussSampler(Random random, int c, double sigma, int tau) {
        super(random, c, sigma, tau);
        upperBound = DiscGaussSamplerFactory.getUpperBound(sigma, tau);
        f = DiscGaussSamplerFactory.getUnitProbability(sigma);
        rho = IntStream.range(0, upperBound)
            .mapToDouble(x -> Math.exp(x * x * f))
            .toArray();
        rho[0] /= 2.0;
    }

    @Override
    public int sample() {
        int x;
        double y;
        do {
            x = random.nextInt(upperBound);
            y = random.nextDouble();
        } while (y >= rho[x]);
        x = random.nextBoolean() ? -x : x;
        return x + c;
    }


    @Override
    public DiscGaussSamplerFactory.DiscGaussSamplerType getType() {
        return DiscGaussSamplerFactory.DiscGaussSamplerType.UNIFORM_TABLE;
    }
}
