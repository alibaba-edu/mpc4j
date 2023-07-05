package edu.alibaba.mpc4j.dp.service.fo.cms;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.AppleCmsFoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Apple's Count Mean Sketch (CMS) Frequency Oracle LDP client. See paper:
 * <p>
 * Differential Privacy Team, Apple. Learning with Privacy at Scale. Technique Report, 2017.
 * </p>
 * The client-side algorithm is as follows.
 * <p>
 * Given a privacy parameter ε > 0 and data entry d ∈ D,
 * <p>1. Sample j uniformly at random from [k].</p>
 * <p>2. Initialize a vector \vec v = (-1, -1, ..., -1) ∈ R^m.</p>
 * <p>3. Set v_{h_j(d)} = 1.</p>
 * <p>4. Sample \vec b ∈ {-1, +1}^m, where each b_l is i.i.d. where Pr[b_l = +1] = e^{ε/2} / (1 + e^{ε/2}).</p>
 * <p>5. \tilde v = (v_1 · b_1, ..., v_m · b_m)</p>
 * <p>6. return \tilde v, j</p>
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/1/31
 */
public class AppleCmsFoLdpClient extends AbstractFoLdpClient {
    /**
     * number of hash functions k
     */
    private final int k;
    /**
     * k byte length
     */
    private final int kByteLength;
    /**
     * the output bound of the hash functions m
     */
    private final int m;
    /**
     * hash seeds
     */
    private final int[] hashSeeds;
    /**
     * the hash function for h_1, ..., h_k
     */
    private final IntHash intHash;
    /**
     * p = 1 / (1 + e^{ε/2})
     */
    private final double p;

    public AppleCmsFoLdpClient(FoLdpConfig config) {
        super(config);
        AppleCmsFoLdpConfig appleCmsFoLdpConfig = (AppleCmsFoLdpConfig) config;
        k = appleCmsFoLdpConfig.getK();
        kByteLength = IntUtils.boundedNonNegIntByteLength(k);
        m = appleCmsFoLdpConfig.getM();
        hashSeeds = appleCmsFoLdpConfig.getHashSeeds();
        intHash = IntHashFactory.fastestInstance();
        p = 1 / (Math.exp(epsilon / 2) + 1);
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        // selects an index j uniformly at random from [k] that corresponds to hash function h_j ∈ H where h_j: D → [m].
        int j = random.nextInt(k);
        byte[] itemIndexBytes = IntUtils.intToByteArray(domain.getItemIndex(item));
        // sets the encoding vector \vec v {-1, 1}^m to be 1 in position h_j(d) and -1 everywhere else.
        int hj = Math.abs(intHash.hash(itemIndexBytes, hashSeeds[j]) % m);
        // for reducing the communication cost, we use a BitVector to represent \vec v, false for -1 and true for 1.
        BitVector bitVector = BitVectorFactory.createZeros(m);
        for (int i = 0; i < m; i++) {
            double u = random.nextDouble();
            if (i != hj) {
                // the correct value is -1
                if (u < p) {
                    // flip the result with probability p
                    bitVector.set(i, true);
                }
            } else {
                // the correct value is 1
                if (u > p) {
                    // set the result with probability 1 - p
                    bitVector.set(i, true);
                }
            }
        }
        // This privatized vector \tilde v along with the index j is what is sent to the server.
        return ByteBuffer.allocate(kByteLength + bitVector.byteNum())
            .put(IntUtils.boundedNonNegIntToByteArray(j, k))
            .put(bitVector.getBytes())
            .array();
    }
}
