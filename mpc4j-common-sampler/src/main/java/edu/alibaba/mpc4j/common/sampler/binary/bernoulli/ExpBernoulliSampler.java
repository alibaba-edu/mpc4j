package edu.alibaba.mpc4j.common.sampler.binary.bernoulli;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.math3.util.Precision;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Bernoulli sampler with p = exp(-γ) for γ >= 0 (so that exp(-γ) ∈ (0, 1]), where
 * <p><ul>
 * <li> Pr[f(x|p) = 1] = p. </li>
 * <li> Pr[f(x|p) = 0] = 1 - p. </li>
 * </ul></p>
 * The algorithm comes from the following paper, Algorithm 1: Algorithm for Sampling Bernoulli(exp(−γ)):
 * <p>
 * Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The discrete gaussian for differential privacy. Advances in
 * Neural Information Processing Systems 33 (2020): 15676-15688.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/03/24
 */
public class ExpBernoulliSampler implements BernoulliSampler {
    /**
     * the random state
     */
    private final Random random;
    /**
     * Bernoulli sampler with p = exp(−1)
     */
    private final BernoulliSampler expNeg1BernoulliSampler;
    /**
     * γ
     */
    private final double gamma;
    /**
     * the success probability p = exp(−γ)
     */
    private final double p;

    public ExpBernoulliSampler(double gamma) {
        this(new SecureRandom(), gamma);
    }

    public ExpBernoulliSampler(Random random, double gamma) {
        assert gamma >= 0 : "γ must be greater or equal than 0: " + gamma;
        this.gamma = gamma;
        p = Math.exp(-gamma);
        this.random = random;
        expNeg1BernoulliSampler = new SecureBernoulliSampler(random, DoubleUtils.EXP_NEGATIVE_1);
    }

    @Override
    public double getP() {
        return p;
    }

    @Override
    public boolean sample() {
        if (Precision.equals(p, 0.0, DoubleUtils.PRECISION)) {
            // p = 0 (γ = +∞)
            return false;
        } else if (Precision.equals(p, 1.0, DoubleUtils.PRECISION)) {
            // p = 1（γ = 0）
            return true;
        }
        if (gamma >= 0 && gamma <= 1) {
            // if γ ∈ [0, 1] then, Set K ← 1.
            int k = 1;
            // loop
            while (true) {
                // Sample A ← Bernoulli(γ/K).
                BernoulliSampler bernoulliSampler = new SecureBernoulliSampler(random, gamma / k);
                boolean a = bernoulliSampler.sample();
                if (a) {
                    // if A = 1 then set K ← K + 1 and continue the loop.
                    k++;
                } else {
                    // if A = 0 then break the loop.
                    break;
                }
            }
            // if K is odd then return 1. if K is even then return 0.
            return k % 2 == 1;
        } else {
            // else, for k = 1 to floor(γ) do
            for (int k = 1; k <= Math.floor(gamma); k++) {
                // Sample B ← Bernoulli(exp(−1))
                boolean b = expNeg1BernoulliSampler.sample();
                // if B = 0 then break the loop and return 0.
                if (!b) {
                    return false;
                }
            }
            // Sample C ← Bernoulli(exp(floor(γ) − γ))
            BernoulliSampler bernoulliSampler = new SecureBernoulliSampler(random, Math.exp(Math.floor(gamma) - gamma));
            // return C.
            return bernoulliSampler.sample();
        }
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        random.setSeed(seed);
    }

    @Override
    public String toString() {
        return "(γ = " + gamma + ")-" + getClass().getSimpleName();
    }
}
