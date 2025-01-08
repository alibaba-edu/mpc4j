package edu.alibaba.mpc4j.crypto.algs.iprf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.math3.exception.MathInternalError;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.special.Beta;
import org.apache.commons.math3.util.FastMath;

import java.util.stream.IntStream;

/**
 * Pseudorandom Multinomial Sampler (PMNS) implementation.
 * <p>
 * A multinomial sampler (MNS) for multinomial distribution MN(n,m) with encoding space K is a triple of efficiently
 * computable functions:
 * <ul>
 * <li>a randomized encoding generation function Gen: {0,1}^* → K.</li>
 * <li>a deterministic function S: K × [n] → [m].</li>
 * <li>a deterministic function S^-1: K × [m] → 2^[n].</li>
 * </ul>
 * The definition and the construction are introduced in Fig. 4 of the following paper:
 * <p>
 * A. Hoover, S. Patel, G. Persiano, K. Yeo. Plinko: Single-Server PIR with Efficient Updates via Invertible PRFs.
 * Cryptology {ePrint} Archive, Paper 2024/318, 2024.
 * </p>
 * Binomial distribution implementation is modified from org.apache.commons.math3.distribution.BinomialDistribution.
 *
 * @author Weiran Liu
 * @date 2024/8/23
 */
class PnmSampler {
    /**
     * pseudorandom function
     */
    private final Prf prf;
    /**
     * input range [0, n)
     */
    private int n;
    /**
     * output range [0, m)
     */
    private int m;
    /**
     * init
     */
    private boolean init;

    /**
     * Creates a pseudo-random multinomial sampler.
     *
     * @param envType environment.
     */
    public PnmSampler(EnvType envType) {
        // we only use PRF to generate double, which uses 32-bit outputs
        prf = PrfFactory.createInstance(envType, Integer.BYTES);
        init = false;
    }

    /**
     * Initializes the sampler.
     *
     * @param n   the input range [0, n).
     * @param m   the output range [0, m).
     * @param key key.
     */
    public void init(int n, int m, byte[] key) {
        MathPreconditions.checkPositive("n", n);
        MathPreconditions.checkPositive("m", m);
        this.n = n;
        this.m = m;
        prf.setKey(key);
        init = true;
    }

    /**
     * Samples x ∈ [0, n) into y ∈ [0, m). Note that the sample result is in order, that is, for any x_1 < x_2, we must
     * have that y_1 <= y_2.
     *
     * @param x x ∈ [0, n)
     * @return y ∈ [0, m).
     */
    public int sample(int x) {
        Preconditions.checkArgument(init);
        MathPreconditions.checkNonNegativeInRange("x", x, n);
        // start ← 0 ; count ← n
        int start = 0;
        int count = n;
        // low ← 0 ; high ← m− 1
        int low = 0;
        int high = m - 1;
        // node ← (start, count, low, high)
        int[] node = new int[]{start, count, low, high};
        // While low < high:
        while (low < high) {
            // (left, right, s) ← children(k, node)
            int[][] sample = children(node);
            int[] left = sample[0];
            int[] right = sample[1];
            int s = sample[2][0];
            if (x < start + s) {
                // If x < start + s then node ← left
                node = left;
            } else {
                // Else node ← right
                node = right;
            }
            // (start, count, low, high) ← node, but we do not need to set count since it is not used in while loop.
            start = node[0];
            low = node[2];
            high = node[3];
        }
        // Return low
        return low;
    }

    /**
     * Inverse Samples y ∈ [0, m) to a set X, where for each x_i ∈ X, we have that x_i ∈ [0, n) and y ← sample(x_i).
     *
     * @param y y ∈ [0, m).
     * @return a set X so that for each x_i ∈ X, x_i ∈ [0, n) and y ← sample(x_i).
     */
    public int[] inverseSample(int y) {
        Preconditions.checkArgument(init);
        MathPreconditions.checkNonNegativeInRange("y", y, m);
        // start ← 0 ; count ← n
        int start = 0;
        int count = n;
        // low ← 0 ; high ← m − 1
        int low = 0;
        int high = m - 1;
        // node ← (start, count, low, high)
        int[] node = new int[]{start, count, low, high};
        // While low < high:
        while (low < high) {
            // (left, right, s) ← children(k, node), but we do not need to set s since it is not used in while loop.
            int[][] sample = children(node);
            int[] left = sample[0];
            int[] right = sample[1];
            // mid ← ⌊(high + low)/2⌋
            int mid = (int) Math.floor((high + low) / 2.0);
            if (y <= mid) {
                // If y ≤ mid then node ← left
                node = left;
            } else {
                // Else node ← right
                node = right;
            }
            // (start, count, low, high) ← node
            start = node[0];
            count = node[1];
            low = node[2];
            high = node[3];
        }
        // Return {start, ..., start + count − 1}
        return IntStream.range(start, start + count).toArray();
    }

    private int[][] children(int[] node) {
        assert node.length == 4;
        // (start, count, low, high) ← node
        int start = node[0];
        int count = node[1];
        int low = node[2];
        int high = node[3];
        // mid ← ⌊(high + low)/2⌋
        int mid = (int) Math.floor((high + low) / 2.0);
        // p ← (mid − low + 1)/(high − low + 1)
        double p = 1.0 * (mid - low + 1) / (high - low + 1);
        // s ← Binomial(count, p; F(k, node))
        int s = binomial(count, p, node);
        // left ← (start, s, low, mid)
        int[] left = new int[]{start, s, low, mid};
        // right ← (start + s, count − s, mid + 1, high)
        int[] right = new int[]{start + s, count - s, mid + 1, high};
        return new int[][]{left, right, new int[]{s}};
    }

    /**
     * Samples according to a Binomial distribution.
     * <p>
     * The binomial distribution with parameters n and p is the discrete probability distribution of the number of
     * successes in a sequence of n independent experiments, each asking a yes–no question, and each with its own
     * Boolean-valued outcome: success (with probability p) or failure (with probability q = 1 - p).
     * <p>
     * The probability of getting exactly k successes in n independent Bernoulli trials (with the same rate p) is given
     * by the probability mass function:
     * <p>$\Pr[X = k] = \binom{n}{k} p^k (1 - p)^{n - k}$</p>
     * for k = 0, 1, 2, ..., n, where $\binom{n}{k} = n! / (k! · (n - k)!)$.
     * <p>
     * The default implementation uses the
     * <a href="http://en.wikipedia.org/wiki/Inverse_transform_sampling">inversion method</a>.
     *
     * @param trials number of trials, i.e., n.
     * @param p      success probability, i.e., p.
     * @param node   seed used to do the sampling. Here node ← (start, count, low, high) is an int array with 4 elements.
     */
    private int binomial(int trials, double p, int[] node) {
        assert node.length == 4;
        return inverseCumulativeProbability(trials, p, prf.getDouble(IntUtils.intArrayToByteArray(node)));
    }

    private int inverseCumulativeProbability(int trials, double p, final double randomDouble) throws OutOfRangeException {
        if (randomDouble < 0.0 || randomDouble > 1.0) {
            throw new OutOfRangeException(randomDouble, 0, 1);
        }

        int lower = getSupportLowerBound(trials, p);
        if (randomDouble == 0.0) {
            return lower;
        }
        if (lower == Integer.MIN_VALUE) {
            if (checkedCumulativeProbability(trials, p, lower) >= randomDouble) {
                return lower;
            }
        } else {
            // this ensures cumulativeProbability(lower) < p, which is important for the solving step
            lower -= 1;
        }

        int upper = getSupportUpperBound(trials, p);
        if (randomDouble == 1.0) {
            return upper;
        }

        // use the one-sided Chebyshev inequality to narrow the bracket.
        final double mu = getNumericalMean(trials, p);
        final double sigma = FastMath.sqrt(getNumericalVariance(trials, p));
        final boolean chebyshevApplies =
            !(Double.isInfinite(mu) || Double.isNaN(mu) || Double.isInfinite(sigma) || Double.isNaN(sigma) || sigma == 0.0);
        if (chebyshevApplies) {
            double k = FastMath.sqrt((1.0 - randomDouble) / randomDouble);
            double tmp = mu - k * sigma;
            if (tmp > lower) {
                lower = ((int) FastMath.ceil(tmp)) - 1;
            }
            k = 1.0 / k;
            tmp = mu + k * sigma;
            if (tmp < upper) {
                upper = ((int) FastMath.ceil(tmp)) - 1;
            }
        }

        return solveInverseCumulativeProbability(trials, p, randomDouble, lower, upper);
    }

    /**
     * This is a utility function. It assumes {@code 0 < randomDouble < 1} and that the inverse cumulative probability
     * lies in the bracket {@code (lower, upper]}. The implementation does simple bisection to find the smallest
     * {@code randomDouble}-quantile <code>inf{x in Z | P(X<=x) >= randomDouble}</code>.
     *
     * @param trials       number of trials, i.e., n.
     * @param p            success probability, i.e., p.
     * @param randomDouble the cumulative probability.
     * @param lower        a value satisfying {@code cumulativeProbability(lower) < randomDouble}.
     * @param upper        a value satisfying {@code randomDouble <= cumulativeProbability(upper)}.
     * @return the smallest {@code randomDouble}-quantile of this distribution.
     */
    private int solveInverseCumulativeProbability(int trials, double p, final double randomDouble, int lower, int upper) {
        while (lower + 1 < upper) {
            int xm = (lower + upper) / 2;
            if (xm < lower || xm > upper) {
                /*
                 * Overflow.
                 * There will never be an overflow in both calculation methods
                 * for xm at the same time
                 */
                xm = lower + (upper - lower) / 2;
            }

            double pm = checkedCumulativeProbability(trials, p, xm);
            if (pm >= randomDouble) {
                upper = xm;
            } else {
                lower = xm;
            }
        }
        return upper;
    }

    /**
     * Computes the cumulative probability function and checks for {@code NaN} values returned. Throws
     * {@code MathInternalError} if the value is {@code NaN}. Rethrows any exception encountered evaluating the
     * cumulative probability function. Throws {@code MathInternalError} if the cumulative probability function
     * returns {@code NaN}.
     *
     * @param trials   number of trials, i.e., n.
     * @param p        success probability, i.e., p.
     * @param argument input value.
     * @return the cumulative probability.
     * @throws MathInternalError if the cumulative probability is {@code NaN}.
     */
    private double checkedCumulativeProbability(int trials, double p, int argument) throws MathInternalError {
        double result = cumulativeProbability(trials, p, argument);
        if (Double.isNaN(result)) {
            throw new MathInternalError(LocalizedFormats.DISCRETE_CUMULATIVE_PROBABILITY_RETURNED_NAN, argument);
        }
        return result;
    }

    /**
     * For a random variable {@code X} whose values are distributed according to the Binomial distribution, this method
     * returns {@code P(X <= x)}. In other words, this method represents the (cumulative) distribution function (CDF)
     * for this distribution.
     *
     * @param trials number of trials, i.e., n.
     * @param p      success probability, i.e., p.
     * @param x      the point at which the CDF is evaluated.
     * @return the probability that a random variable with this distribution takes a value less than or equal to {@code x}.
     */
    private double cumulativeProbability(int trials, double p, int x) {
        double ret;
        if (x < 0) {
            ret = 0.0;
        } else if (x >= trials) {
            ret = 1.0;
        } else {
            ret = 1.0 - Beta.regularizedBeta(p, x + 1.0, trials - x);
        }
        return ret;
    }

    /**
     * The lower bound of the support is always 0 except for the probability parameter {@code p = 1}.
     *
     * @param trials number of trials, i.e., n.
     * @param p      success probability, i.e., p.
     * @return lower bound of the support (0 or the number of trials).
     */
    private int getSupportLowerBound(int trials, double p) {
        return p < 1.0 ? 0 : trials;
    }

    /**
     * The upper bound of the support is the number of trials except for the probability parameter {@code p = 0}.
     *
     * @param trials number of trials, i.e., n.
     * @param p      success probability, i.e., p.
     * @return upper bound of the support (number of trials or 0)
     */
    private int getSupportUpperBound(int trials, double p) {
        return p > 0.0 ? trials : 0;
    }

    /**
     * For number of {@code trials} and probability parameter {@code p}, the mean is {@code n * p}.
     *
     * @param trials number of trials, i.e., n.
     * @param p      success probability, i.e., p.
     */
    public double getNumericalMean(int trials, double p) {
        return trials * p;
    }

    /**
     * For number of {@code trials} and probability parameter {@code p}, the variance is {@code n * p * (1 - p)}.
     *
     * @param trials number of trials, i.e., n.
     * @param p      success probability, i.e., p.
     */
    public double getNumericalVariance(int trials, double p) {
        return trials * p * (1 - p);
    }
}
