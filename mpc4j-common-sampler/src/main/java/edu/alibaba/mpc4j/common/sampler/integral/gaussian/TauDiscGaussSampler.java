package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

/**
 * The interface of Discrete Gaussian Sampling with a cut-off parameter τ.
 * <p>
 * Samples outside the range `(⌊c⌉ - ⌈στ⌉,...,⌊c⌉ + ⌈στ⌉)` are considered to have probability zero.
 * </p>
 * This bound applies to algorithms which sample from the uniform distribution.
 *
 * @author Weiran Liu
 * @date 2022/11/25
 */
public interface TauDiscGaussSampler extends DiscGaussSampler {
    /**
     * Get the cut-off parameter τ.
     *
     * @return the cut-off parameter τ.
     */
    int getTau();
}
