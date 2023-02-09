package edu.alibaba.mpc4j.dp.service.fo.hadamard;

import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Hadamard Mechanism (HM) Frequency Oracle LDP client. This is the original Hadamard Mechanism. See paper:
 * <p>
 * Cormode, Graham, Samuel Maddock, and Carsten Maple. "Frequency estimation under local differential privacy.
 * VLDB 2021, no. 11, pp. 2046-2058.
 * </p>
 * The original Hadamard Mechanism samples t = 1 Boolean value as the report. The client description is as follows:
 * <p>
 * Given a user’s input x_i ∈ [d], we sample an index j ∈ [d], and compute the (scaled-up) coefficient θ_j^(i)
 * = φ_{x_i, j} = (-1)^{&lt;x_i, j&gt;}, where &lt;&gt; means bit-wise inner product. Then with probability
 * p = e^ε / (1 + e^ε), the mechanism reports (j, θ_j^(i)), otherwise it reports (j, -θ_j^(i)).
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/1/30
 */
public class HmFoLdpClient extends AbstractFoLdpClient {
    /**
     * the Hadamard matrix size, the smallest exponent of 2 that is bigger than d
     */
    private final int n;
    /**
     * n byte length
     */
    private final int nByteLength;
    /**
     * p = e^ε / (e^ε + 1)
     */
    private final double p;

    public HmFoLdpClient(FoLdpConfig config) {
        super(config);
        // the smallest exponent of 2 which is bigger than d
        int k = LongUtils.ceilLog2(d + 1);
        n = 1 << k;
        nByteLength = IntUtils.boundedNonNegIntByteLength(n);
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + 1);
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        int x = domain.getItemIndex(item) + 1;
        // sample an index j. The paper states that j ∈ [d]. However, it seems that correct way is j ∈ [n]
        int j = random.nextInt(n);
        // compute (-1)^<x_i, j>
        boolean hadamardCoefficient = Integer.bitCount(x & j) % 2 == 0;
        double u = random.nextDouble();
        if (u > p) {
            // with probability 1 - e^ε / (1 + e^ε), the mechanism reports (j, -θ_j^(i)).
            hadamardCoefficient = !hadamardCoefficient;
        }
        return ByteBuffer.allocate(nByteLength + 1)
            .put(IntUtils.boundedNonNegIntToByteArray(j, n))
            .put(hadamardCoefficient ? (byte)1 : (byte)-1)
            .array();
    }
}
