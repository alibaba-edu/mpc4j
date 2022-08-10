package edu.alibaba.mpc4j.s2pc.pso.oprf.cm20;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigDecimalUtils;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * CM20-MPOPRF工具类。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfUtils {
    /**
     * 私有构造函数。
     */
    private Cm20MpOprfUtils() {
        // empty
    }

    /**
     * W取值查找表
     */
    private static final Map<Integer, Integer> W_INIT_MATRIX = new HashMap<>();

    static {
        W_INIT_MATRIX.put(8, 585);
        W_INIT_MATRIX.put(9, 588);
        W_INIT_MATRIX.put(10, 591);
        W_INIT_MATRIX.put(11, 594);
        W_INIT_MATRIX.put(12, 597);
        W_INIT_MATRIX.put(13, 600);
        W_INIT_MATRIX.put(14, 603);
        W_INIT_MATRIX.put(15, 606);
        W_INIT_MATRIX.put(16, 609);
        W_INIT_MATRIX.put(17, 612);
        W_INIT_MATRIX.put(18, 615);
        W_INIT_MATRIX.put(19, 618);
        W_INIT_MATRIX.put(20, 621);
        W_INIT_MATRIX.put(21, 624);
        W_INIT_MATRIX.put(22, 627);
        W_INIT_MATRIX.put(23, 630);
        W_INIT_MATRIX.put(24, 633);
    }

    /**
     * 得到w的值，见原始论文表1。
     *
     * @param maxBatchSize 最大批处理数量。
     * @return w的值。
     */
    public static int getW(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^8
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 8));
        if (W_INIT_MATRIX.containsKey(nLogValue)) {
            return W_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
            "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 24));
    }

    /**
     * 搜索w的下界为计算安全常数，这是因为d等于计算安全常数
     */
    private static final int W_LOWER_BOUND = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * 搜索w的上界为8倍计算安全常数
     */
    private static final int W_UPPER_BOUND = 8 * CommonConstants.BLOCK_BIT_LENGTH;

    /**
     * 根据批处理数量计算数w。
     *
     * @param n 最大批处理数量。
     * @return w的值。
     */
    public static int searchW(int n) {
        // 要求n > 1，否则p = (1 - 1 / n)^n等于0
        assert n > 1;
        return searchW(n, (W_LOWER_BOUND + W_UPPER_BOUND) / 2, W_LOWER_BOUND, W_UPPER_BOUND);
    }

    private static int searchW(final int n, int currentW, int lowerW, int higherW) {
        // lowerW <= currentW <= higherW
        assert currentW >= lowerW && currentW <= higherW;
        // lowerW >= W_LOWER_BOUND，higherW <= W_UPPER_BOUND
        assert lowerW >= W_LOWER_BOUND && higherW <= W_UPPER_BOUND;
        // lowerW是当前不满足安全要求的已知最小w，higherW是当前满足安全要求的已知最大w，如果只差1，说明higherW就是要找的结果
        if (higherW - lowerW == 1) {
            return higherW;
        }
        BigDecimal baseNegl = calBaseNegl(n, currentW);
        BigDecimal maliciousNegl = calMaliciousNegl(n, currentW);
        if (baseNegl.compareTo(BigDecimalUtils.STATS_NEG_PROG) < 0
            && maliciousNegl.compareTo(BigDecimalUtils.BLOCK_NEG_PROB) < 0) {
            // 如果都小于指定概率，意味着currentW是满足安全要求的
            higherW = currentW;
        } else {
            // 如果有一个不小于指定概率，意味着currentW不满足安全要求
            lowerW = currentW;
        }
        // 二分查找，迭代搜索
        currentW = (lowerW + higherW) / 2;
        return searchW(n, currentW, lowerW, higherW);
    }

    private static BigDecimal calBaseNegl(final int n, int w) {
        // 对于任意i \in [1, w], j \in [1, n]，都有Pr[D_i[j] = 1] = (1 - 1 / m)^{n_2}
        BigDecimal pBigDecimal = BigDecimal.valueOf(Math.pow(1 - 1.0 / n, n))
            .setScale(BigDecimalUtils.PRECISION, BigDecimalUtils.ROUNDING_MODE);
        BigDecimal negl = BigDecimal.ZERO;
        for (int k = 0; k <= CommonConstants.BLOCK_BIT_LENGTH - 1; k++) {
            negl = negl.add(pBigDecimal.pow(k)
                .multiply(BigDecimal.ONE.subtract(pBigDecimal).pow(w - k))
                .multiply(new BigDecimal(BigIntegerUtils.combinatorial(w, k))));
        }
        return negl.multiply(BigDecimal.valueOf(n));
    }

    private static BigDecimal calMaliciousNegl(long n, int w) {
        // negl = (1 / 2 + 1 / (2n))^w
        return BigDecimalUtils.HALF
            .add(BigDecimalUtils.HALF.divide(BigDecimal.valueOf(n), BigDecimalUtils.ROUNDING_MODE))
            .pow(w);
    }
}
