package edu.alibaba.mpc4j.dp.service.fo.hadamard;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Hadamard Mechanism (HM) Frequency Oracle LDP server. This is the optimized Hadamard Mechanism. See paper:
 * <p>
 * Cormode, Graham, Samuel Maddock, and Carsten Maple. "Frequency estimation under local differential privacy.
 * VLDB 2021, no. 11, pp. 2046-2058.
 * </p>
 * The paper shown above introduce an optimization for Hadamard Mechanism.
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
public class HmLowEpsFoLdpServer extends AbstractFoLdpServer {
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
     * t byte length
     */
    private final int tByteLength;
    /**
     * p = e^ε / (e^ε + 2^t - 1)
     */
    private final double p;
    /**
     * the budgets
     */
    private final int[] budgets;

    public HmLowEpsFoLdpServer(FoLdpConfig config) {
        super(config);
        // the smallest exponent of 2 which is bigger than d
        int k = LongUtils.ceilLog2(d + 1);
        n = 1 << k;
        nByteLength = IntUtils.boundedNonNegIntByteLength(n);
        double expEpsilon = Math.exp(epsilon);
        // the optimal t = log_2(e^ε + 1)
        t = (int)Math.ceil(DoubleUtils.log2(expEpsilon + 1));
        assert t >= 1 : "t must be greater than or equal to 1: " + t;
        tByteLength = CommonUtils.getByteLength(t);
        // p = e^ε / (e^ε + 2^t - 1)
        p = expEpsilon / (expEpsilon + (1 << t) - 1);
        budgets = new int[n];
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, nByteLength * t + tByteLength
        );
        byte[] jBytes = new byte[nByteLength];
        int[] jArray = new int[t];
        for (int i = 0; i < t; i++) {
            System.arraycopy(itemBytes, i * nByteLength, jBytes, 0, jBytes.length);
            jArray[i] = IntUtils.byteArrayToBoundedNonNegInt(jBytes, n);
            MathPreconditions.checkNonNegativeInRange("j_" + i, jArray[i], n);
        }
        byte[] coefficientBytes = new byte[tByteLength];
        System.arraycopy(itemBytes, nByteLength * t, coefficientBytes, 0, coefficientBytes.length);
        BitVector coefficients = BitVectorFactory.create(t, coefficientBytes);
        for (int i = 0; i < t; i++) {
            int hadamardCoefficient = coefficients.get(i) ? 1 : -1;
            budgets[jArray[i]] += hadamardCoefficient;
        }
        num++;
    }

    @Override
    public Map<String, Double> estimate() {
        int[] cs = HadamardCoder.fastWalshHadamardTrans(budgets);
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> {
                    // map to x
                    int x = itemIndex + 1;
                    // map to C(x)
                    int cx = cs[x];
                    // p(x) = C(x) / (2p - 1) / t
                    return cx / (2 * p - 1) / t;
                }
            ));
    }
}
