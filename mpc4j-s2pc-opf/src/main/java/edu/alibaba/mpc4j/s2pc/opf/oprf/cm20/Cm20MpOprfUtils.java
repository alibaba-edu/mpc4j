package edu.alibaba.mpc4j.s2pc.opf.oprf.cm20;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigDecimalUtils;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CM20-MP-OPRF utilities.
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfUtils {
    /**
     * private constructor.
     */
    private Cm20MpOprfUtils() {
        // empty
    }

    /**
     * w lower bound
     */
    private static final int W_LOWER_BOUND = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * w upper bound
     */
    private static final int W_UPPER_BOUND = 8 * CommonConstants.BLOCK_BIT_LENGTH;

    /**
     * search w.
     *
     * @param n n.
     * @return w.
     */
    public static int searchW(int n) {
        // n > 1, otherwise p = (1 - 1 / n)^n = 0
        MathPreconditions.checkGreater("n", n, 1);
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
            .setScale(BigDecimalUtils.PRECISION, RoundingMode.HALF_UP);
        BigDecimal negl = BigDecimal.ZERO;
        for (int k = 0; k <= CommonConstants.BLOCK_BIT_LENGTH - 1; k++) {
            negl = negl.add(pBigDecimal.pow(k)
                .multiply(BigDecimal.ONE.subtract(pBigDecimal).pow(w - k))
                .multiply(new BigDecimal(BigIntegerUtils.binomial(w, k))));
        }
        return negl.multiply(BigDecimal.valueOf(n));
    }

    private static BigDecimal calMaliciousNegl(long n, int w) {
        // negl = (1 / 2 + 1 / (2n))^w
        return BigDecimalUtils.HALF
            .add(BigDecimalUtils.HALF.divide(BigDecimal.valueOf(n), RoundingMode.HALF_UP))
            .pow(w);
    }
}
