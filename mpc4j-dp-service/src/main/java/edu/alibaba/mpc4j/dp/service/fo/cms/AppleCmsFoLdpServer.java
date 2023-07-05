package edu.alibaba.mpc4j.dp.service.fo.cms;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.AppleCmsFoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Apple's Count Mean Sketch (CMS) Frequency Oracle LDP client. See paper:
 * <p>
 * Differential Privacy Team, Apple. Learning with Privacy at Scale. Technique Report, 2017.
 * </p>
 * The server-side algorithm for computing the sketch matrix is as follows.
 * <p>
 * For the i-th element submitted by the client with format (j, \vec v):
 * <p>1. Set c_ε = (e^{ε / 2} + 1) / (e^{ε / 2} - 1).</p>
 * <p>2. Set \tilde {\vec x}^(i) = k·(c_ε/2 · \tilde{\vec v}^(i) + 1/2*\vec {1}).</p>
 * <p>3. for l ∈ [m], M_{j, l} = M_{j, l} + \tilde x_l</p>
 * </p>
 * The server-side algorithm for aggregate the result is to simply de-biased results for each input d by computing:
 * <p>
 * (m / (m - 1)) * (1 / k * Σ_{l = 1}^k (M[l][h_l(d)]) - n / m)
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/2/1
 */
public class AppleCmsFoLdpServer extends AbstractFoLdpServer {
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
     * the byte size of m
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
     * c_ε = (e^{ε / 2} + 1) / (e^{ε / 2} - 1)
     */
    private final double cEpsilon;
    /**
     * the budget
     */
    private final double[][] budget;

    public AppleCmsFoLdpServer(FoLdpConfig config) {
        super(config);
        AppleCmsFoLdpConfig appleCmsFoLdpConfig = (AppleCmsFoLdpConfig) config;
        k = appleCmsFoLdpConfig.getK();
        kByteLength = IntUtils.boundedNonNegIntByteLength(k);
        m = appleCmsFoLdpConfig.getM();
        mByteLength = CommonUtils.getByteLength(m);
        budget = new double[k][m];
        hashSeeds = appleCmsFoLdpConfig.getHashSeeds();
        intHash = IntHashFactory.fastestInstance();
        // c_ε = (e^{ε / 2} + 1) / (e^{ε / 2} - 1)
        double expHalfEpsilon = Math.exp(epsilon / 2);
        cEpsilon = (expHalfEpsilon + 1) / (expHalfEpsilon - 1);
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, kByteLength + mByteLength
        );
        // read j
        byte[] jBytes = new byte[kByteLength];
        System.arraycopy(itemBytes, 0, jBytes, 0, jBytes.length);
        int j = IntUtils.byteArrayToBoundedNonNegInt(jBytes, k);
        MathPreconditions.checkNonNegativeInRange("j", j, k);
        // read the vector
        byte[] mBytes = new byte[mByteLength];
        System.arraycopy(itemBytes, kByteLength, mBytes, 0, mBytes.length);
        BitVector bitVector = BitVectorFactory.create(m, mBytes);
        // Set \tilde {\vec x}^(i) = k·(c_ε/2 · \tilde{\vec v}^(i) + 1/2*\vec {1})
        double[] vectorX = IntStream.range(0, m)
            .mapToDouble(l -> {
                double v = bitVector.get(l) ? 1.0 : -1.0;
                return k * (cEpsilon / 2 * v + 0.5);
            })
            .toArray();
        // for l ∈ [m], M_{j, l} = M_{j, l} + \tilde x_l
        for (int l = 0; l < m; l++) {
            budget[j][l] += vectorX[l];
        }
        num++;
    }

    @Override
    public Map<String, Double> estimate() {
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> {
                    byte[] itemIndexBytes = IntUtils.intToByteArray(itemIndex);
                    double aggregate = 0.0;
                    // Σ_{l = 1}^k (M[l][h_l(d)])
                    for (int l = 0; l < k; l++) {
                        int hl = Math.abs(intHash.hash(itemIndexBytes, hashSeeds[l]) % m);
                        aggregate += budget[l][hl];
                    }
                    // (m / (m - 1)) * (1 / k * Σ_{l = 1}^k (M[l][h_l(d)]) - n / m).
                    return (double)m / (m - 1) * ((1.0 / k) * aggregate - num / m);
                }
            ));
    }
}
