package edu.alibaba.mpc4j.common.sampler.integral.others;

import edu.alibaba.mpc4j.common.sampler.Sampler;

import java.util.Random;

/**
 * Discrete Gaussian D_{σ₂, 0}` with `σ₂ := sqrt(1 / (2·log(2)))`.
 * <p>
 * Return integer `x` with probability `ρ_{σ, c}(x) = exp(-(x - c)² / (2σ₂²)) / exp(-(\ZZ - c)² / (2σ₂²))`.
 * </p>
 * where
 * <p>
 * `exp(-(\ZZ - c)² / (2σ₂²)) ≈ \sum_{i = -τσ₂}^{τσ₂} exp(-(i - c)² / (2σ₂²))` is the probability for all of the integers.
 * </p>
 * Modified from:
 * <p>
 * https://github.com/malb/dgs/blob/master/dgs/dgs_gauss_dp.c
 * </p>
 * The algorithm is described in the following paper, Algorithm 10:
 * <p>
 * Ducas, Léo, Alain Durmus, Tancrède Lepoint, and Vadim Lyubashevsky. Lattice signatures and bimodal Gaussians.
 * CRYPTO 2013, pp. 40-56. Springer, Berlin, Heidelberg, 2013.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/27
 */
public class Sigma2DiscGaussSampler implements Sampler {
    /**
     * σ₂ := sqrt(1 / (2·log(2)))
     */
    public static final double SIGMA_2 = Math.sqrt(1.0 / (2 * Math.log(2.0)));
    /**
     * the random state
     */
    protected final Random random;

    public Sigma2DiscGaussSampler(Random random) {
        this.random = random;
    }

    public int sample() {
        while (true) {
            // generate a big b ← Bernoulli(1/2), if b = 0 then return 0
            if (!random.nextBoolean()) {
                return 0;
            }
            // for i = 1 to ∞ do
            boolean doBreak = false;
            // the case for i = 1, here k = 2 * 1 - 1 = 1, there is only one random bit b1. if b1 = 0, then return i
            if (!random.nextBoolean()) {
                return 1;
            }
            for (int i = 2; ; i++) {
                // draw random bits b_1 ... b_k for k = 2 * i - 1
                for (int k = 0; k < 2 * i - 2; k++) {
                    if (random.nextBoolean()) {
                        doBreak = true;
                        break;
                    }
                }
                // if b_1 ... b_k != 0 ... 0 then restart
                if (doBreak) {
                    break;
                }
                // if b_k = 0, then return i
                if (!random.nextBoolean()) {
                    return i;
                }
            }
        }
    }

    @Override
    public double getMean() {
        throw new UnsupportedOperationException("Cannot get the mean μ since it depends on the input integer x");
    }

    @Override
    public double getVariance() {
        throw new UnsupportedOperationException("Cannot get the variance σ^2 since it depends on the input integer x");
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        random.setSeed(seed);
    }
}
