package edu.alibaba.mpc4j.common.tool.hashbin;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * 哈希桶工具类。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class MaxBinSizeUtils {
    /**
     * 统计安全常数所对应的可忽略函数值
     */
    public static final BigDecimal STAT_NEG_PROBABILITY = BigDecimal.valueOf(0.5).pow(CommonConstants.STATS_BIT_LENGTH);
    /**
     * 小数点后保留20位即可以满足统计安全常数的计算准确性
     */
    private static final int STRUCTURE_SCALE = 20;

    /**
     * 根据桶的个数、元素的个数，计算每个桶估计的大小。算法参见下述论文的第3.1节：
     *  Pinkas B, Schneider T, Zohner M. Scalable private set intersection based on OT extension. ACM Transactions on
     *  Privacy and Security (TOPS), 2018, 21(2): 1-35.
     *
     * @param b 桶的个数。
     * @param n 元素的个数。
     * @return 每个桶估计的大小。
     */
    public static int expectMaxBinSize(final int n, final int b) {
        assert b > 0 && n > 0;
        if (b == 1) {
            return n;
        }
        // 先将桶中元素的最大数量设置为1或者n/b的最大值
        int k = Math.max(1, n / b + 1);
        // 先计算一轮溢出概率，如果上来满足要求，则直接返回k
        double probability = expectProbability(n, b, k);
        if (probability <= DoubleUtils.STATS_NEG_PROBABILITY) {
            return k;
        }
        int step = 1;
        // 应用二分查找算法找到最接近给定统计安全常数的桶大小
        boolean doubling = true;
        while (probability > DoubleUtils.STATS_NEG_PROBABILITY || step > 1) {
            if (probability > DoubleUtils.STATS_NEG_PROBABILITY) {
                // 如果当前溢出概率大于要求溢出概率，意味着桶的大小设置得太小，需要增加
                if (doubling) {
                    step = Math.max(1, step * 2);
                } else {
                    step = Math.max(1, step / 2);
                }
                k += step;
            } else {
                // 桶的大小设置得太大，需要减小。减小的时候要一点一点降低
                doubling = false;
                step = Math.max(1, step / 2);
                k -= step;
            }
            probability = expectProbability(n, b, k);
        }
        return k;
    }

    /**
     * 计算将n个元素随机放置在b个桶中，包含元素最多的桶中至少包含k个元素的估计概率。
     *
     * @param b 桶的个数。
     * @param n 元素的个数。
     * @param k 最多的桶中至少包含k个元素。
     * @return n个元素随机放置在b个桶中，包含元素最多的桶中至少包含k个元素的估计概率。
     */
    private static double expectProbability(int n, int b, int k) {
        if (n <= k) {
            // 如果元素的个数小于每个桶至少包含的元素的个数，则概率为0，因此回复的结果是double的最大值
            return 0.0;
        }
        // Pr <= b * (en / bk)^k
        return b * Math.pow(Math.E * n / (b * k), k);
    }

    /**
     * 根据桶的个数、元素的个数，计算每个桶严格的大小。算法参见下述论文的第5.2节：
     * Garimella, Gayathri, et al. Oblivious key-value stores and amplification for private set intersection.
     * CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
     *
     * @param b 桶的个数。
     * @param n 元素的个数。
     * @return 每个桶严格的大小。
     */
    public static int exactMaxBinSize(final int n, final int b) {
        assert b > 0 && n > 0;
        if (b == 1) {
            return n;
        }
        // 先将桶中元素的最大数量设置为1或者n/b的最大值，这样迭代速度可以快一些
        int k = Math.max(1, n / b);
        // 先计算一轮溢出概率，如果上来满足要求，则直接返回k
        BigDecimal probability = exactProbability(n, b, k);
        if (probability.compareTo(STAT_NEG_PROBABILITY) <= 0) {
            return k;
        }
        int step = 1;
        // 应用二分查找算法找到最接近给定统计安全常数的桶大小
        boolean doubling = true;
        while (probability.compareTo(STAT_NEG_PROBABILITY) > 0 || step > 1) {
            if (probability.compareTo(STAT_NEG_PROBABILITY) > 0) {
                // 如果当前溢出概率大于要求溢出概率，意味着桶的大小设置得太小，需要增加
                if (doubling) {
                    step = Math.max(1, step * 2);
                } else {
                    step = Math.max(1, step / 2);
                }
                k += step;
            } else {
                // 桶的大小设置得太大，需要减小。减小的时候要一点一点降低
                doubling = false;
                step = Math.max(1, step / 2);
                k -= step;
            }
            probability = exactProbability(n, b, k);
        }
        return k;
    }

    /**
     * 计算将n个元素随机放置在b个桶中，包含元素最多的桶中至少包含k个元素的准确概率。
     *
     * @param b 桶的个数。
     * @param n 元素的个数。
     * @param k 最多的桶中至少包含k个元素。
     * @return n个元素随机放置在b个桶中，包含元素最多的桶中至少包含k个元素的概率。
     */
    private static BigDecimal exactProbability(int n, int b, int k) {
        if (n <= k) {
            // 如果元素的个数小于每个桶至少包含的元素的个数，则概率为0
            return BigDecimal.ZERO;
        }
        // q
        BigDecimal binBigDecimal = BigDecimal.valueOf(b).setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP);
        // 1 / q
        BigDecimal binInverseBigDecimal = BigDecimal.ONE.setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP)
            .divide(binBigDecimal, RoundingMode.HALF_UP)
            .setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP);
        // 1 - 1 / q
        BigDecimal oneMinusBinInverseBigDecimal = BigDecimal.ONE.setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP)
            .subtract(binInverseBigDecimal)
            .setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP);
        BigDecimal probability = BigDecimal.ZERO
            .setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP);
        for (int i = k; i <= n; i++) {
            BigInteger combinatorial = BigIntegerUtils.combinatorial(n, i);
            probability = probability.add(
                new BigDecimal(combinatorial).setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP)
                    .multiply(binInverseBigDecimal.pow(i)).setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP)
                    .multiply(oneMinusBinInverseBigDecimal.pow(n - i)).setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP)
                    .setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP)
            );
        }
        return binBigDecimal.multiply(probability);
    }
}
