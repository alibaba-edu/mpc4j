package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import java.util.Random;

/**
 * Abstract Discrete Gaussian Sampler with a cut-off parameter τ.
 *
 * @author Weiran Liu
 * @date 2022/11/25
 */
abstract class AbstractDiscGaussSampler implements DiscGaussSampler {
    /**
     * the random state
     */
    protected final Random random;
    /**
     * the mean of the distribution c
     */
    protected final int c;
    /**
     * the width parameter σ
     */
    protected final double sigma;

    AbstractDiscGaussSampler(Random random, int c, double sigma) {
        assert sigma > 0 : "σ must be greater than 0" + sigma;
        this.c = c;
        this.sigma = sigma;
        this.random = random;
    }

    @Override
    public int getC() {
        return c;
    }

    @Override
    public double getInputSigma() {
        return sigma;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        random.setSeed(seed);
    }

    @Override
    public String toString() {
        return "(c = " + c + ", σ = " + sigma + ")-" + getType().name();
    }
}
