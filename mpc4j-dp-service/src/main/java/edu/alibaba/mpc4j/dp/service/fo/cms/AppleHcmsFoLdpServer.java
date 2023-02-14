package edu.alibaba.mpc4j.dp.service.fo.cms;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.AppleHcmsFoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Apple's Hadamard Count Mean Sketch (HCMS) Frequency Oracle LDP client. See paper:
 * <p>
 * Differential Privacy Team, Apple. Learning with Privacy at Scale. Technique Report, 2017.
 * </p>
 * The server-side algorithm for computing the sketch matrix is as follows.
 * <p>
 * For the i-th element submitted by the client with format (j, \vec v):
 * <p>1. Set c_ε = (e^ε + 1) / (e^ε - 1).</p>
 * <p>2. Set \tilde {x}^(i) = k · c_ε · \tilde{w}^(i).</p>
 * <p>3. M_{j, l} = M_{j, l} + \tilde {x}^(i)</p>
 * </p>
 * The server-side algorithm for aggregate the result is to first transform the rows of sketch back as M = M · H_m^T,
 * then simply de-biased results for each input d by computing:
 * <p>
 * (m / (m - 1)) * (1 / k * Σ_{l = 1}^k (M[l][h_l(d)]) - n / m)
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/2/1
 */
public class AppleHcmsFoLdpServer extends AbstractFoLdpServer {
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
     * c_ε = (e^ε + 1) / (e^ε - 1)
     */
    private final double cEpsilon;
    /**
     * the budget
     */
    private final double[][] budget;

    public AppleHcmsFoLdpServer(FoLdpConfig config) {
        super(config);
        AppleHcmsFoLdpConfig appleHcmsFoLdpConfig = (AppleHcmsFoLdpConfig) config;
        k = appleHcmsFoLdpConfig.getK();
        kByteLength = IntUtils.boundedNonNegIntByteLength(k);
        m = appleHcmsFoLdpConfig.getM();
        mByteLength = IntUtils.boundedNonNegIntByteLength(m);
        budget = new double[k][m];
        hashSeeds = appleHcmsFoLdpConfig.getHashSeeds();
        intHash = IntHashFactory.fastestInstance();
        // c_ε = (e^ε + 1) / (e^ε - 1)
        double expEpsilon = Math.exp(epsilon);
        cEpsilon = (expEpsilon + 1) / (expEpsilon - 1);
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, 1 + kByteLength + mByteLength
        );
        // read w
        byte w = itemBytes[0];
        Preconditions.checkArgument(w == (byte)1 || w == (byte)-1);
        // read j
        byte[] jBytes = new byte[kByteLength];
        System.arraycopy(itemBytes, 1, jBytes, 0, jBytes.length);
        int j = IntUtils.byteArrayToBoundedNonNegInt(jBytes, k);
        MathPreconditions.checkNonNegativeInRange("j", j, k);
        // read l
        byte[] lBytes = new byte[mByteLength];
        System.arraycopy(itemBytes, 1 + kByteLength, lBytes, 0, lBytes.length);
        int l = IntUtils.byteArrayToBoundedNonNegInt(lBytes, m);
        MathPreconditions.checkNonNegativeInRange("l", l, m);
        // set x = k · c_ε · w
        double x = k * cEpsilon * w;
        // M_{j, l} = M_{j, l} + \tilde {x}^(i)
        budget[j][l] += x;
        num++;
    }

    @Override
    public Map<String, Double> estimate() {
        // transform the rows of sketch back as M = M · H_m^T
        double[][] sketch = Arrays.stream(budget)
            .map(HadamardCoder::fastWalshHadamardTrans)
            .toArray(double[][]::new);
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> {
                    byte[] itemIndexBytes = IntUtils.intToByteArray(itemIndex);
                    double aggregate = 0.0;
                    // Σ_{l = 1}^k (M[l][h_l(d)])
                    for (int l = 0; l < k; l++) {
                        int hl = Math.abs(intHash.hash(itemIndexBytes, hashSeeds[l]) % (m - 1)) + 1;
                        aggregate += sketch[l][hl];
                    }
                    // (m / (m - 1)) * (1 / k * Σ_{l = 1}^k (M[l][h_l(d)]) - n / m).
                    return (double)m / (m - 1) * ((1.0 / k) * aggregate - num / m);
                }
            ));
    }
}
