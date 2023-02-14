package edu.alibaba.mpc4j.dp.service.fo.cms;

import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.AppleHcmsFoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Apple's Hadamard Count Mean Sketch (HCMS) Frequency Oracle LDP client. See paper:
 * <p>
 * Differential Privacy Team, Apple. Learning with Privacy at Scale. Technique Report, 2017.
 * </p>
 * The client-side algorithm is as follows.
 * <p>
 * Given a privacy parameter ε > 0 and data entry d ∈ D,
 * <p>1. Sample j uniformly at random from [k].</p>
 * <p>2. Initialize a vector \vec v = (0, 0, ..., 0) ∈ Z^m.</p>
 * <p>3. Set v_{h_j(d)} = 1.</p>
 * <p>4. Transform \vec w = H_m · \vec v, where H_m is the Hadamard Matrix with dimension m >= d and m = 2^k. </p>
 * <p>5. Sample l uniformly at random from [m].</p>
 * <p>6. Sample b ∈ {-1, +1}, which is +1 with probability e^ε / (1 + e^ε)</p>
 * <p>7. Set \tilde w = b · w_l</p>
 * <p>8. return \tilde w, j, l</p>
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/2/1
 */
public class AppleHcmsFoLdpClient extends AbstractFoLdpClient {
    /**
     * number of hash functions k
     */
    private final int k;
    /**
     * k byte length
     */
    private final int kByteLength;
    /**
     * the output bound of the hash functions m, must be a power of 2
     */
    private final int m;
    /**
     * m byte length
     */
    private final int mByteLength;
    /**
     * hash seeds
     */
    private final int[] hashSeeds;
    /**
     * the hash function for h_1, ..., h_k
     */
    private final IntHash intHash;
    /**
     * p = 1 / (1 + e^ε)
     */
    private final double p;

    public AppleHcmsFoLdpClient(FoLdpConfig config) {
        super(config);
        AppleHcmsFoLdpConfig appleHcmsFoLdpConfig = (AppleHcmsFoLdpConfig) config;
        k = appleHcmsFoLdpConfig.getK();
        kByteLength = IntUtils.boundedNonNegIntByteLength(k);
        m = appleHcmsFoLdpConfig.getM();
        mByteLength = IntUtils.boundedNonNegIntByteLength(m);
        hashSeeds = appleHcmsFoLdpConfig.getHashSeeds();
        intHash = IntHashFactory.fastestInstance();
        // p = 1 / (1 + e^ε)
        p = 1 / (Math.exp(epsilon) + 1);
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        // Sample j uniformly at random from [k].
        int j = random.nextInt(k);
        // h_j(d)
        byte[] itemIndexBytes = IntUtils.intToByteArray(domain.getItemIndex(item));
        int hj = Math.abs(intHash.hash(itemIndexBytes, hashSeeds[j]) % (m - 1)) + 1;
        // Initialize a vector \vec v = (0, 0, ..., 0) ∈ Z^m, set v_{h_j(d)} = 1, transform \vec w = H_m · \vec v,
        // sample l uniformly at random from [m], and get w_l.
        // The above procedures can be seen as sample l uniformly at random from [m], and w_l = H[hj][l].
        int l = random.nextInt(m);
        boolean wl = HadamardCoder.checkParity(hj, l);
        // Sample b ∈ {-1, +1}, which is +1 with probability e^ε / (1 + e^ε), Set \tilde w = b · w_l
        double u = random.nextDouble();
        if (u < p) {
            wl = !wl;
        }
        // return \tilde w, j, l
        return ByteBuffer.allocate(1 + kByteLength + mByteLength)
            .put(wl ? (byte)1 : (byte)-1)
            .put(IntUtils.boundedNonNegIntToByteArray(j, k))
            .put(IntUtils.boundedNonNegIntToByteArray(l, m))
            .array();
    }
}
