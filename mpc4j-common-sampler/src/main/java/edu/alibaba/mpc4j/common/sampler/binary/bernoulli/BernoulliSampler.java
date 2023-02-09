package edu.alibaba.mpc4j.common.sampler.binary.bernoulli;

import edu.alibaba.mpc4j.common.sampler.binary.BinarySampler;

/**
 * Bernoulli sampler with p ∈ [0, 1], where
 * <p><ul>
 * <li> Pr[f(x|p) = 1] = p. </li>
 * <li> Pr[f(x|p) = 0] = 1 - p. </li>
 * </ul></p>
 *
 * @author Weiran Liu
 * @date 2021/12/27
 */
public interface BernoulliSampler extends BinarySampler {
    /**
     * Get the success probability p。
     *
     * @return the success probability p。
     */
    double getP();

    /**
     * Get the mean μ. In Bernoulli sampling, μ = p.
     *
     * @return the mean μ = p.
     */
    @Override
    default double getMean() {
        return getP();
    }

    /**
     * Get the variance σ^2. In Bernoulli sampling, σ^2 = p * (1 - p).
     *
     * @return the variance σ^2 = p * (1 - p).
     */
    @Override
    default double getVariance() {
        return getP() * (1.0 - getP());
    }
}
