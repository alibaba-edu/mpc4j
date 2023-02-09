package edu.alibaba.mpc4j.dp.service.fo.hadamard;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Hadamard Mechanism (HM) Frequency Oracle LDP server. This is the original Hadamard Mechanism. See paper:
 * <p>
 * Cormode, Graham, Samuel Maddock, and Carsten Maple. "Frequency estimation under local differential privacy.
 * VLDB 2021, no. 11, pp. 2046-2058.
 * </p>
 * The original Hadamard Mechanism samples t = 1 Boolean value as the report. The server description is as follows:
 * <p>
 * To build an unbiased estimator for frequencies, we take the contribution of the inverse of the unbiased estimator of
 * the reported θ'_j = Σ (θ_j^(i)). The unbiased estimator for θ_j is θ'_j / (2p - 1). For a given x, to estimate f(x),
 * we sum the contribution to f(x) from all reports.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/1/30
 */
public class HmFoLdpServer extends AbstractFoLdpServer {
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
    /**
     * the budgets
     */
    private final int[] budgets;

    public HmFoLdpServer(FoLdpConfig config) {
        super(config);
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + 1);
        // the smallest exponent of 2 which is bigger than d
        int k = LongUtils.ceilLog2(d + 1);
        n = 1 << k;
        nByteLength = IntUtils.boundedNonNegIntByteLength(n);
        budgets = new int[n];
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, nByteLength + 1
        );
        byte[] jBytes = new byte[nByteLength];
        System.arraycopy(itemBytes, 0, jBytes, 0, jBytes.length);
        int j = IntUtils.byteArrayToBoundedNonNegInt(jBytes, n);
        MathPreconditions.checkNonNegativeInRange("j", j, n);
        byte theta = itemBytes[nByteLength];
        Preconditions.checkArgument(theta == (byte)1 || theta == (byte)-1);
        budgets[j] += theta;
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
                    // p(x) = C(x) / (2p - 1)
                    return cx / (2 * p - 1);
                }
            ));
    }
}
