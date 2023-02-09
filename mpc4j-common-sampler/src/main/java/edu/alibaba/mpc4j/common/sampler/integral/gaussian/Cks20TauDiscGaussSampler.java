package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import java.util.Random;

/**
 * Discrete Gaussian sampling with a cut-off parameter τ, proposed by Canonne, Kamath and Steinke, described in the
 * following paper:
 * <p>
 * Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The discrete gaussian for differential privacy. Advances in
 * Neural Information Processing Systems 33 (2020): 15676-15688.
 * </p>
 * The algorithm is described in Section 5.3, Algorithm 3: Algorithm for Sampling a Discrete Gaussian.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
class Cks20TauDiscGaussSampler extends Cks20DiscGaussSampler implements TauDiscGaussSampler {
    /**
     * Cutoff `τ`, samples outside the range `(⌊c⌉ - ⌈στ⌉, ..., ⌊c⌉ + ⌈στ⌉)` are considered to have probability zero.
     */
    private final int tau;
    /**
     * We sample x with abs(x) < upper_bound
     */
    private final int upperBound;

    /**
     * Init CKS20 Discrete Gaussian Sampler with a cut-off parameter τ.
     *
     * @param random the random state.
     * @param c      the mean of the distribution c.
     * @param sigma  the width parameter σ.
     * @param tau   the cut-off parameter τ.
     */
    public Cks20TauDiscGaussSampler(Random random, int c, double sigma, int tau) {
        super(random, c, sigma);
        assert tau > 0 : "τ must be greater than 0: " + tau;
        this.tau = tau;
        upperBound = DiscGaussSamplerFactory.getUpperBound(sigma, tau);
    }

    @Override
    public int sample() {
        int sample;
        do {
            sample = super.sample();
        } while (sample - c <= -1 * upperBound || sample >= upperBound);
        return sample;
    }

    @Override
    public int getTau() {
        return tau;
    }

    @Override
    public DiscGaussSamplerFactory.DiscGaussSamplerType getType() {
        return DiscGaussSamplerFactory.DiscGaussSamplerType.CKS20_TAU;
    }
}
