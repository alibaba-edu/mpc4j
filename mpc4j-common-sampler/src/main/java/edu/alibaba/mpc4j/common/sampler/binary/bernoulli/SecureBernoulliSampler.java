package edu.alibaba.mpc4j.common.sampler.binary.bernoulli;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.math3.util.Precision;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Bernoulli sampler with p ∈ [0, 1] using Random provided by JDK, where
 * <p><ul>
 * <li> Pr[f(x|p) = 1] = p. </li>
 * <li> Pr[f(x|p) = 0] = 1 - p. </li>
 * </ul></p>
 *
 * @author Weiran Liu
 * @date 2022/03/25
 */
public class SecureBernoulliSampler implements BernoulliSampler {
    /**
     * the random state
     */
    private final Random random;
    /**
     * the success probability
     */
    private final double p;

    public SecureBernoulliSampler(double p) {
        this(new SecureRandom(), p);
    }

    public SecureBernoulliSampler(Random random, double p) {
        assert p >= 0 && p <= 1 : "p must be in range [0, 1]: " + p;
        this.random = random;
        this.p = p;
    }

    @Override
    public double getP() {
        return p;
    }

    @Override
    public boolean sample() {
        if (Precision.equals(p, 0.0, DoubleUtils.PRECISION)) {
            return false;
        } else if (Precision.equals(p, 1.0, DoubleUtils.PRECISION)) {
            return true;
        }
        double u = random.nextDouble();

        return u <= p;
    }

    @Override
    public void reseed(long seed) {
        random.setSeed(seed);
    }

    @Override
    public String toString() {
        return "(p = " + p + ")-" + getClass().getSimpleName();
    }
}
