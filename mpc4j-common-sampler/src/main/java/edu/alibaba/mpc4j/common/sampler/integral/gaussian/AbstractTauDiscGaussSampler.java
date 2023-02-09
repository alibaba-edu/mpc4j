package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import java.util.Random;

/**
 * Abstract Discrete Gaussian Sampler with a cut-off parameter τ.
 *
 * @author Weiran Liu
 * @date 2022/11/25
 */
abstract class AbstractTauDiscGaussSampler extends AbstractDiscGaussSampler implements TauDiscGaussSampler {
    /**
     * Cutoff `τ`, samples outside the range `(⌊c⌉ - ⌈στ⌉, ..., ⌊c⌉ + ⌈στ⌉)` are considered to have probability zero.
     */
    protected final int tau;

    AbstractTauDiscGaussSampler(Random random, int c, double sigma, int tau) {
        super(random, c, sigma);
        assert tau > 0 : "τ must be greater than 0: " + tau;
        this.tau = tau;
    }

    @Override
    public int getTau() {
        return tau;
    }
}
