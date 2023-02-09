package edu.alibaba.mpc4j.dp.service.tool;

import edu.alibaba.mpc4j.common.tool.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static edu.alibaba.mpc4j.common.tool.utils.BigDecimalUtils.STRUCTURE_SCALE;

/**
 * 指纹工具类。
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class FingerprintUtils {

    private FingerprintUtils() {
        // empty
    }

    /**
     * 计算将m个元素放置在w个桶中后，元素的指纹比特长度l，即要求：
     * <p>
     * 1 - (1 - 2^{-l})^{w / m} <= 2^{-40}
     * </p>
     *
     * @param m m个元素。
     * @param w w个桶。
     * @return 指纹比特长度。
     */
    public static int fingerprintBitLength(int m, int w) {
        assert m >= 0 : "m must be greater than or equal to 0: " + m;
        assert w > 0 : "w must be greater than 0: " + w;
        if (m == 0 || m == 1) {
            // 1个元素，一定不发生碰撞，指纹为1
            return 1;
        }
        // 初始化一个l
        int l = (int) Math.ceil((double) m / w);
        // 先计算一轮碰撞概率
        BigDecimal probability = collisionProbability(m, w, l);
        int step = 1;
        // 应用二分查找算法找到最接近给定统计安全常数的桶大小
        boolean doubling = true;
        while (probability.compareTo(BigDecimalUtils.STAT_NEG_PROBABILITY) > 0 || step > 1) {
            if (probability.compareTo(BigDecimalUtils.STAT_NEG_PROBABILITY) > 0) {
                // 如果当前碰撞概率大于要求碰撞概率，意味着l太小，需要增加
                if (doubling) {
                    step = Math.max(1, step * 2);
                } else {
                    step = Math.max(1, step / 2);
                }
                l += step;
            } else {
                // l太大，需要减小。减小的时候要一点一点降低
                doubling = false;
                step = Math.max(1, step / 2);
                l -= step;
            }
            probability = collisionProbability(m, w, l);
        }
        return l;
    }

    /**
     * 计算将m个元素压缩到l比特后放置在w个桶中，发生碰撞的概率。
     *
     * @param m m个元素。
     * @param w w个桶。
     * @param l l比特。
     * @return 发生碰撞的概率。
     */
    private static BigDecimal collisionProbability(int m, int w, int l) {
        assert m > 0 && w > 0 && l > 0;
        // M / w
        int exp = (int) Math.ceil((double) m / w);
        // (1 - 2^{-l})^{M / w}
        return BigDecimal.ONE.subtract(
            BigDecimal.ONE.subtract(BigDecimalUtils.HALF.setScale(STRUCTURE_SCALE, RoundingMode.HALF_UP).pow(l)).pow(exp)
        );
    }
}
