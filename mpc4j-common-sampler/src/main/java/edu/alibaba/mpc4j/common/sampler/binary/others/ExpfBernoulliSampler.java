package edu.alibaba.mpc4j.common.sampler.binary.others;

import edu.alibaba.mpc4j.common.sampler.Sampler;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.math3.util.Precision;

import java.util.Random;

/**
 * Bernoulli with p = exp(-x/f) for integers, where x ∈ [0, 2^l). Modified from:
 * <p>
 * https://github.com/malb/dgs/blob/master/dgs/dgs_bern.c
 * </p>
 * The algorithm is described in the following paper, Algorithm 8:
 * <p>
 * Ducas, Léo, Alain Durmus, Tancrède Lepoint, and Vadim Lyubashevsky. Lattice signatures and bimodal Gaussians.
 * CRYPTO 2013, pp. 40-56. Springer, Berlin, Heidelberg, 2013.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/27
 */
public class ExpfBernoulliSampler implements Sampler {
    /**
     * The block size for the number of inner Bernoulli samplers
     */
    private static final int BERNOULLI_EXP_BLOCK_SIZE = 16;
    /**
     * the random state
     */
    private final Random random;
    /**
     * x ∈ [0, 2^l)
     */
    private int l;
    /**
     * The inner Bernoulli samplers
     */
    private SecureBernoulliSampler[] bernoulliSamplers;

    public ExpfBernoulliSampler(Random random, double f, int upperBound) {
        assert upperBound > 0 : "upperBound must be greater than 0:" + upperBound;
        int maxL = 2 * (int)Math.ceil(DoubleUtils.log2(upperBound));
        this.random = random;
        if (maxL == 0) {
            maxL = Long.SIZE - 1;
        }
        l = BERNOULLI_EXP_BLOCK_SIZE;
        bernoulliSamplers = new SecureBernoulliSampler[l];
        // compute c_i = exp(-2^i / f) for 0 <= i <= l - 1
        double lnci = -1.0 / f;
        double ci;
        for (int i = 0; i < l; i++) {
            // c_i = exp(-2^i / f)
            ci = Math.exp(lnci);
            if (Precision.equals(ci, 0.0, DoubleUtils.PRECISION)) {
                l = i;
                break;
            }
            if (i % BERNOULLI_EXP_BLOCK_SIZE == 0 && i != 0) {
                l += BERNOULLI_EXP_BLOCK_SIZE;
                l = Math.min(maxL, l);
                bernoulliSamplers = new SecureBernoulliSampler[l];
            }
            // c_i = exp(-2^i / f)
            bernoulliSamplers[i] = new SecureBernoulliSampler(random, ci);
            lnci = 2 * lnci;
        }
        if (maxL < l) {
            l = maxL;
        }
    }

    public boolean sample(int x) {
        assert x >= 0 : "x must be greater than or equal to 0: " + x;
        if (x == 0) {
            return true;
        }
        // we treat x ∈ [0, 2^l) in binary form x = x_{l - 1} ... x_0
        // for i = l − 1 to 0
        for (int i = l - 1; i >= 0; i--) {
            // if x_i = 1 then
            if ((x & (1L << i)) != 0L) {
                // sample A_i := B_{c_i}, if A_0 = 0 then return 0
                if (!bernoulliSamplers[i].sample()) {
                    return false;
                }
            }
        }
        // return 1
        return true;
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
