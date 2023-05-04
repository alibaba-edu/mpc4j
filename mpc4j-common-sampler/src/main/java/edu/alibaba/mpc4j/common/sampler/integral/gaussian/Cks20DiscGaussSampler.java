package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.integral.geometric.DiscreteGeometricSampler;

import java.util.Random;

/**
 * Discrete Gaussian sampling, proposed by Canonne, Kamath and Steinke, described in the following paper:
 * <p>
 * Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The discrete gaussian for differential privacy. Advances in
 * Neural Information Processing Systems 33 (2020): 15676-15688.
 * </p>
 * The algorithm is described in Section 5.3, Algorithm 3: Algorithm for Sampling a Discrete Gaussian.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
class Cks20DiscGaussSampler extends AbstractDiscGaussSampler {
    /**
     * Discrete Laplace sampler
     */
    private final DiscreteGeometricSampler discreteGeometricSampler;
    /**
     * σ^2
     */
    private final double sigmaSquare;
    /**
     * t = ⌊σ⌋ + 1
     */
    private final int t;

    /**
     * Init CKS20 Discrete Gaussian Sampler.
     *
     * @param random the random state.
     * @param c      the mean of the distribution c.
     * @param sigma  the width parameter σ.
     */
    public Cks20DiscGaussSampler(Random random, int c, double sigma) {
        super(random, c, sigma);
        sigmaSquare = sigma * sigma;
        t = (int) Math.floor(sigma) + 1;
        discreteGeometricSampler = new DiscreteGeometricSampler(random, 0, t, 1);
    }

    @Override
    public int sample() {
        while (true) {
            // Sample Y ← Lap_Z(t)
            int y = discreteGeometricSampler.sample();
            // Sample C ← Bernoulli(exp(−(|Y| − σ^2/t)^2 / 2σ^2)).
            double gamma = Math.pow(Math.abs(y) - sigmaSquare / t, 2) / 2.0 / sigmaSquare;
            ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(random, gamma);
            boolean c = expBernoulliSampler.sample();
            if (c) {
                // If C = 1, return Y as output.
                return y + this.c;
            }
            // If C = 0, reject and restart.
        }
    }

    @Override
    public DiscGaussSamplerFactory.DiscGaussSamplerType getType() {
        return DiscGaussSamplerFactory.DiscGaussSamplerType.CKS20;
    }
}
