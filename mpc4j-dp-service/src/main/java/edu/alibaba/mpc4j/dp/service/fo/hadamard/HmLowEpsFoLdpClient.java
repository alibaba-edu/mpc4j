package edu.alibaba.mpc4j.dp.service.fo.hadamard;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Hadamard Mechanism (HM) Frequency Oracle LDP client. This is the optimized Hadamard Mechanism. See paper:
 * <p>
 * Cormode, Graham, Samuel Maddock, and Carsten Maple. "Frequency estimation under local differential privacy.
 * VLDB 2021, no. 11, pp. 2046-2058.
 * </p>
 * The paper shown above introduce an optimization for Hadamard Mechanism running on the client.
 * <p>
 * To improve the bound when e^ε is large we can sample t Hadamard coefficients to produce a hash function with g = 2^t
 * possible outcomes. This preserves the result with probability p^* = e^ε / (e^ε + 2^t - 1), and otherwise perturbs it
 * uniformly.
 * </p>
 * The experiments show that the optimized Hadamard Mechanism is better when ε is small.
 *
 * @author Weiran Liu
 * @date 2023/1/30
 */
public class HmLowEpsFoLdpClient extends AbstractFoLdpClient {
    /**
     * the Hadamard matrix size, the smallest exponent of 2 that is bigger than d
     */
    private final int n;
    /**
     * n byte length
     */
    private final int nByteLength;
    /**
     * 2^t - 1 = e^ε, so that t = log_2(e^ε + 1).
     */
    private final int t;
    /**
     * p = e^ε / (e^ε + 2^t - 1)
     */
    private final double p;

    public HmLowEpsFoLdpClient(FoLdpConfig config) {
        super(config);
        // the smallest exponent of 2 which is bigger than d
        int k = LongUtils.ceilLog2(d + 1);
        n = 1 << k;
        nByteLength = IntUtils.boundedNonNegIntByteLength(n);
        double expEpsilon = Math.exp(epsilon);
        // the optimal t = log_2(e^ε + 1)
        t = (int)Math.ceil(DoubleUtils.log2(expEpsilon + 1));
        assert t >= 1 : "t must be greater than or equal to 1: " + t;
        // p = e^ε / (e^ε + 2^t - 1)
        p = expEpsilon / (expEpsilon + (1 << t) - 1);
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        int x = domain.getItemIndex(item) + 1;
        int[] jArray = new int[t];
        BitVector coefficients = BitVectorFactory.createZeros(t);
        for (int i = 0; i < t; i++) {
            // sample an index j. The paper states that j ∈ [d]. However, it seems that correct way is j ∈ [n]
            jArray[i] = random.nextInt(n);
            // compute (-1)^<x_i, j>
            boolean coefficient = Integer.bitCount(x & jArray[i]) % 2 == 0;
            coefficients.set(i, coefficient);
        }
        double u = random.nextDouble();
        if (u > p) {
            BitVector randomCoefficients = null;
            boolean success = false;
            while (!success) {
                // preserves the result with probability p^* = e^ε / (e^ε + 2^t - 1), and otherwise perturbs uniformly.
                randomCoefficients = BitVectorFactory.createRandom(t, random);
                success = !coefficients.equals(randomCoefficients);
            }
            coefficients = randomCoefficients;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(t * nByteLength + coefficients.byteNum());
        for (int i = 0; i < t; i++) {
            byteBuffer.put(IntUtils.boundedNonNegIntToByteArray(jArray[i], n));
        }
        byteBuffer.put(coefficients.getBytes());
        return byteBuffer.array();
    }
}
