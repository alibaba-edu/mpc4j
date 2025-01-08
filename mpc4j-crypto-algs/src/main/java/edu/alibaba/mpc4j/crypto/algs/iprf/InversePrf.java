package edu.alibaba.mpc4j.crypto.algs.iprf;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.crypto.algs.smprp.SmallDomainPrp;
import edu.alibaba.mpc4j.crypto.algs.smprp.SmallDomainPrpFactory;

/**
 * Inverse pseudo-random function. This cryptographic primitive is formally defined in the following paper:
 * <p>
 * A. Hoover, S. Patel, G. Persiano, K. Yeo. Plinko: Single-Server PIR with Efficient Updates via Invertible PRFs.
 * Cryptology {ePrint} Archive, Paper 2024/318, 2024.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/10/9
 */
public class InversePrf {
    /**
     * Pseudorandom Multinomial Sampler
     */
    private final PnmSampler pnmSampler;
    /**
     * small-domain PRP
     */
    private final SmallDomainPrp smallDomainPrp;

    /**
     * Creates a inverse pseudo-random function.
     *
     * @param envType environment.
     */
    public InversePrf(EnvType envType) {
        pnmSampler = new PnmSampler(envType);
        smallDomainPrp = SmallDomainPrpFactory.createInstance(envType);
    }

    /**
     * Initializes the pseudo-random function.
     *
     * @param n   the input range [0, n).
     * @param m   the output range [0, m).
     * @param key key.
     */
    public void init(int n, int m, byte[] key) {
        // here we re-use key
        pnmSampler.init(n, m, key);
        smallDomainPrp.init(n, key);
    }

    /**
     * Gets PRF evaluation.
     *
     * @param x x ∈ [0, n).
     * @return y ∈ [0, m).
     */
    public int prf(int x) {
        return pnmSampler.sample(smallDomainPrp.prp(x));
    }

    /**
     * Gets inverse PRF evaluation.
     *
     * @param y y ∈ [0, m).
     * @return a set X so that for each x_i ∈ X, x_i ∈ [0, n) and y ← PRF(x_i).
     */
    public int[] inversePrf(int y) {
        int[] xs = pnmSampler.inverseSample(y);
        for (int i = 0; i < xs.length; i++) {
            xs[i] = smallDomainPrp.invPrp(xs[i]);
        }
        return xs;
    }
}
