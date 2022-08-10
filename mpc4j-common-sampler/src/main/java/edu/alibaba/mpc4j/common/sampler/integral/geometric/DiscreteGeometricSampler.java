package edu.alibaba.mpc4j.common.sampler.integral.geometric;

import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 离散Laplace分布采样，方案来自于下述论文的第5.2节，Algorithm 2: Algorithm for Sampling a Discrete Laplace：
 * <p>
 * Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The discrete gaussian for differential privacy. Advances in
 * Neural Information Processing Systems 33 (2020): 15676-15688.
 * <p>
 * Proposition 32:
 * <p>
 * On input s, t ∈ Z with s, t ≥ 1, the procedure described in Algorithm 2 outputs one sample from Lap_Z(t/s), and
 * requires a constant number of operations in expectation.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
public class DiscreteGeometricSampler implements GeometricSampler {
    /**
     * 随机数生成器
     */
    private final Random random;
    /**
     * A采样器
     */
    private final ExpBernoulliSampler aSampler;
    /**
     * B采样器
     */
    private final SecureBernoulliSampler bSampler;
    /**
     * 均值μ
     */
    private final int mu;
    /**
     * t ∈ Z, t ≥ 1
     */
    private final int t;
    /**
     * s ∈ Z, s ≥ 1
     */
    private final int s;

    /**
     * 构建离散双边几何机制采样器。
     *
     * @param mu 均值μ。
     * @param t  参数t，使得b = t / s。
     * @param s  参数s，使得b = t / s。
     */
    public DiscreteGeometricSampler(int mu, int t, int s) {
        this(new SecureRandom(), mu, t, s);
    }

    /**
     * 构建离散双边几何机制采样器。
     *
     * @param random 随机数生成器。
     * @param mu     均值μ。
     * @param t      参数t，使得b = t / s。
     * @param s      参数s，使得b = t / s。
     */
    public DiscreteGeometricSampler(Random random, int mu, int t, int s) {
        assert t >= 1 : "t must be greater or equal to 1";
        assert s >= 1 : "s must be greater or equal to 1";
        this.mu = mu;
        this.t = t;
        this.s = s;
        this.random = random;
        aSampler = new ExpBernoulliSampler(random, 1.0);
        bSampler = new SecureBernoulliSampler(random, 0.5);
    }

    @Override
    public int sample() {
        int u;
        while (true) {
            // Sample U ∈ {0, 1, 2, · · · , t − 1} uniformly at random.
            u = random.nextInt(t);
            // Sample D ← Bernoulli(exp(−U/t)).
            ExpBernoulliSampler dSampler = new ExpBernoulliSampler(random, (double) u / t);
            boolean d = dSampler.sample();
            if (!d) {
                // if D = 0 then reject and restart.
                continue;
            }
            // Initialize V ← 0.
            int v = 0;
            while (true) {
                // Sample A ← Bernoulli(exp(−1)).
                boolean a = aSampler.sample();
                if (!a) {
                    // if A = 0 then break the loop.
                    break;
                } else {
                    // if A = 1 then set V ← V + 1 and continue.
                    v++;
                }
            }
            // Set X ← U + t · V.
            int x = u + t * v;
            // Set Y ← ⌊X/s⌋
            int y = (int) Math.floor((double) x / s);
            // Sample B ← Bernoulli(1/2).
            boolean b = bSampler.sample();
            if (b && y == 0) {
                // if B = 1 and Y = 0 then reject and restart.
                continue;
            }
            // return Z ← (1 − 2B) · Y.
            return mu + (1 - 2 * (b ? 1 : 0)) * y;
        }
    }

    @Override
    public int getMu() {
        return mu;
    }

    @Override
    public double getB() {
        return (double) t / s;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        random.setSeed(seed);
    }

    @Override
    public String toString() {
        return "(μ = " + getMu() + ", b = " + getB() + ")-" + getClass().getSimpleName();
    }
}
