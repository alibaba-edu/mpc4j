package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;

import java.util.Arrays;
import java.util.Random;

/**
 * Discrete Gaussian Sampler using the Alias method. See https://en.wikipedia.org/wiki/Alias_method for more details.
 * <p>
 * Setup costs are roughly σ^2 (as currently implemented) and table sizes linear in σ, but sampling is then just a
 * randomized lookup. Any real-valued c is accepted.
 * </p>
 * Modified from:
 * <p>
 * https://github.com/malb/dgs/blob/master/dgs/dgs_gauss_dp.c
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/28
 */
class AliasTauDiscGaussSampler extends AbstractTauDiscGaussSampler {
    /**
     * We sample x with abs(x) <= upper_bound - 1
     */
    private final int upperBoundMinusOne;
    /**
     * There are 2 * upper_bound - 1 elements in the range [-upper_bound + 1, ..., upper_bound - 1]
     */
    private final int twoUpperBoundMinusOne;
    /**
     * The probability table for the normalized exp(-(x - 2)² / (2σ²))
     */
    private final double[] rho;
    /**
     * The alias table
     */
    private final int[] alias;
    /**
     * Bias sampler
     */
    private final SecureBernoulliSampler[] bias;

    /**
     * Init Allias Discrete Gaussian Sampler.
     *
     * @param random the random state.
     * @param c      the mean of the distribution c.
     * @param sigma  the width parameter σ.
     * @param tau    the cut-off parameter τ.
     */
    AliasTauDiscGaussSampler(Random random, int c, double sigma, int tau) {
        super(random, c, sigma, tau);
        int upperBound = DiscGaussSamplerFactory.getUpperBound(sigma, tau);
        upperBoundMinusOne = upperBound - 1;
        twoUpperBoundMinusOne = 2 * upperBound - 1;
        double f = DiscGaussSamplerFactory.getUnitProbability(sigma);
        rho = new double[twoUpperBoundMinusOne];
        for (int x = -upperBoundMinusOne; x <= upperBoundMinusOne; x++) {
            rho[x + upperBoundMinusOne] = Math.exp(x * x * f);
        }
        // convert rho to probabilities
        double sum = Arrays.stream(rho).sum();
        sum = 1.0 / sum;
        for (int x = 0; x < twoUpperBoundMinusOne; x++) {
            rho[x] *= sum;
        }
        // compute bias and alias
        alias = new int[twoUpperBoundMinusOne];
        bias = new SecureBernoulliSampler[twoUpperBoundMinusOne];
        // simple robin hood strategy approximates good alias.
        // this pre-computation takes ~n^2, but could be reduced by using better data structures to compute min and max
        // (instead of just linear search each time)
        double avg = 1.0 / twoUpperBoundMinusOne;
        int low = getLowestRhoIndex();
        int high;
        while (avg - rho[low] > DiscGaussSamplerFactory.STRONG_EQUAL_PRECISION) {
            high = getHighestRhoIndex();
            bias[low]  = new SecureBernoulliSampler(random, twoUpperBoundMinusOne * rho[low]);
            alias[low] = high;
            rho[high] -= (avg - rho[low]);
            rho[low] = avg;
            low = getLowestRhoIndex();
        }
    }

    private int getLowestRhoIndex() {
        int lowestRhoIndex = 0;
        double lowestRho = rho[lowestRhoIndex];
        for (int index = 1; index < twoUpperBoundMinusOne; index++) {
            if (rho[index] < lowestRho) {
                lowestRhoIndex = index;
                lowestRho = rho[lowestRhoIndex];
            }
        }
        return lowestRhoIndex;
    }

    private int getHighestRhoIndex() {
        int highestRhoIndex = 0;
        double highestRho = rho[highestRhoIndex];
        for (int index = 1; index < twoUpperBoundMinusOne; index++) {
            if (rho[index] > highestRho) {
                highestRhoIndex = index;
                highestRho = rho[highestRhoIndex];
            }
        }
        return highestRhoIndex;
    }

    @Override
    public DiscGaussSamplerFactory.DiscGaussSamplerType getType() {
        return DiscGaussSamplerFactory.DiscGaussSamplerType.ALIAS;
    }

    @Override
    public int sample() {
        // generate a uniformly random i ∈ [-U, U]
        int i = random.nextInt(twoUpperBoundMinusOne);
        // Note that the distribution may be padded with additional probabilities p_i = 0
        // to increase n to a convenient value, such as a power of two.
        if (bias[i] != null) {
            // generate a uniformly random y ∈ [0, 1)
            if (!bias[i].sample()) {
                // If y < U_i, return i, this is the biased coin flip, otherwise, return K_i
                i = alias[i];
            }
        }
        return i + c - upperBoundMinusOne;
    }


}
