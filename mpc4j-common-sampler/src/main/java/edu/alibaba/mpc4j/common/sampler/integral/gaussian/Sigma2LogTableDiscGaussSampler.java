package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import edu.alibaba.mpc4j.common.sampler.binary.others.ExpfBernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.integral.others.Sigma2DiscGaussSampler;

import java.util.Random;

/**
 * σ₂ Log Table Discrete Gaussian Sampler.
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
class Sigma2LogTableDiscGaussSampler extends AbstractDiscGaussSampler {
    /**
     * actual σ
     */
    protected final double actualSigma;
    /**
     * the round integer k = 「σ / σ₂」
     */
    private final int k;
    /**
     * Bernoulli with p = exp(-x/f) for integers
     */
    private final ExpfBernoulliSampler expfBernoulliSampler;
    /**
     * Discrete Gaussian D_{σ₂, 0}` with `σ₂ := sqrt(1 / (2·log(2)))`
     */
    private final Sigma2DiscGaussSampler sigma2DiscGaussSampler;

    /**
     * Init Uniform Log Table Discrete Gaussian Sampler.
     *
     * @param random the random state.
     * @param c      the mean of the distribution c.
     * @param sigma  the width parameter σ.
     */
    Sigma2LogTableDiscGaussSampler(Random random, int c, double sigma) {
        super(random, c, sigma);
        k = (int)Math.round(sigma / Sigma2DiscGaussSampler.SIGMA_2);
        actualSigma = k * Sigma2DiscGaussSampler.SIGMA_2;
        double f = 2 * sigma * sigma;
        int upperBound = DiscGaussSamplerFactory.getUpperBound(actualSigma, DiscGaussSamplerFactory.DEFAULT_TAU);
        expfBernoulliSampler = new ExpfBernoulliSampler(random, f, upperBound);
        sigma2DiscGaussSampler = new Sigma2DiscGaussSampler(random);
    }

    @Override
    public DiscGaussSamplerFactory.DiscGaussSamplerType getType() {
        return DiscGaussSamplerFactory.DiscGaussSamplerType.SIGMA2_LOG_TABLE;
    }

    @Override
    public int sample() {
        int x, y, z;
        do {
            do {
                // sample x ∈ Z according to D_{σ₂}^+
                x = sigma2DiscGaussSampler.sample();
                // sample y ∈ Z uniformly in {0, ..., k - 1}
                y = random.nextInt(k);
                // sample b ← ExpfBernoulli(-y * (y + 2kx) / (2σ^2))
            } while (!expfBernoulliSampler.sample(y * (y + 2 * k * x)));
            // z ← kx + y
            z = k * x + y;
            if (z == 0) {
                // if z = 0, restart with probability 1/2
                if (random.nextBoolean()) {
                    // do not restart
                    break;
                }
                // restart
            } else {
                // do not restart
                break;
            }
        } while (true);
        // generate a bit b ← Bernoulli(1/2) and return (-1)^b * z
        if (random.nextBoolean()) {
            z = -z;
        }
        return z + c;
    }

    @Override
    public double getActualSigma() {
        return actualSigma;
    }
}
