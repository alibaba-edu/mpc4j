package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import java.util.Random;

/**
 * σ₂ Log Table Discrete Gaussian Sampler with a cut-off parameter τ.
 * <p>
 * Samples are drawn from an easily samplable distribution with $σ = k · σ₂$ where $σ₂ := \sqrt{1 / (2\log 2)}$ and
 * accepted with probability proportional to $\exp(-(x - c)² / (2σ²))$ where $\exp(-(x - c)² / (2σ²))$ is computed using
 * logarithmically many calls to Bernoulli distributions (but no calls to $\exp$). Note that this sampler adjusts σ to
 * match $σ₂·k$ for some integer $k$. Only integer-valued $c$ are supported.
 * </p>
 * Modified from:
 * <p>
 * https://github.com/malb/dgs/blob/master/dgs/dgs_gauss_dp.c
 * </p>
 * The algorithm is described in the following paper, Algorithm 11 and Algorithm 12:
 * <p>
 * Ducas, Léo, Alain Durmus, Tancrède Lepoint, and Vadim Lyubashevsky. Lattice signatures and bimodal Gaussians.
 * CRYPTO 2013, pp. 40-56. Springer, Berlin, Heidelberg, 2013.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/27
 */
class Sigma2LogTableTauDiscGaussSampler extends Sigma2LogTableDiscGaussSampler implements TauDiscGaussSampler {
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
    public Sigma2LogTableTauDiscGaussSampler(Random random, int c, double sigma, int tau) {
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
        return DiscGaussSamplerFactory.DiscGaussSamplerType.SIGMA2_LOG_TABLE_TAU;
    }
}
