package edu.alibaba.mpc4j.dp.service.fo.hadamard;

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
 * Hadamard Response (HR) Frequency Oracle LDP server. This is the standard HR mechanism implementation. See paper:
 * <p>
 * Acharya, Jayadev, Ziteng Sun, and Huanyu Zhang. Hadamard response: Estimating distributions privately, efficiently,
 * and with little communication. AISATAS 2019, pp. 1120-1129. PMLR, 2019.
 * </p>
 * Although the standard HR mechanism is more suitable for low ε, we do not restrict ε to be very low.
 *
 * @author Weiran Liu
 * @date 2023/1/19
 */
public class HrFoLdpServer extends AbstractFoLdpServer {
    /**
     * the Hadamard matrix size, the smallest exponent of 2 that is bigger than d, which also equals to the output size.
     */
    private final int n;
    /**
     * n byte length
     */
    private final int nByteLength;
    /**
     * e^ε
     */
    private final double expEpsilon;
    /**
     * the budgets
     */
    private final int[] budgets;

    public HrFoLdpServer(FoLdpConfig config) {
        super(config);
        // the smallest exponent of 2 which is bigger than d
        int k = LongUtils.ceilLog2(d + 1);
        n = 1 << k;
        nByteLength = IntUtils.boundedNonNegIntByteLength(n);
        expEpsilon = Math.exp(epsilon);
        budgets = new int[n];
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, nByteLength
        );
        int y = IntUtils.byteArrayToBoundedNonNegInt(itemBytes, n);
        MathPreconditions.checkNonNegativeInRange("y", y, n);
        budgets[y]++;
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
                    // p(x) = (e^ε + 1) / (e^ε - 1) * C(x)
                    return cx * ((expEpsilon + 1) / (expEpsilon - 1));
                }
            ));
    }
}
