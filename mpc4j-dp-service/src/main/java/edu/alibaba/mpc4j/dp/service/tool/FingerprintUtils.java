package edu.alibaba.mpc4j.dp.service.tool;

import edu.alibaba.mpc4j.common.tool.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * fingerprint utilities.
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class FingerprintUtils {
    /**
     * private constructor.
     */
    private FingerprintUtils() {
        // empty
    }

    /**
     * Gets fingerprint bit length l when putting m elements into w bins, using the following formula:
     * <p>
     * 1 - (1 - 2^{-l})^{w / m} <= 2^{-40}
     * </p>
     *
     * @param m m elements.
     * @param w w bins.
     * @return fingerprint bit length.
     */
    public static int fingerprintBitLength(int m, int w) {
        assert m >= 0 : "m must be greater than or equal to 0: " + m;
        assert w > 0 : "w must be greater than 0: " + w;
        if (m == 0 || m == 1) {
            return 1;
        }
        int l = (int) Math.ceil((double) m / w);
        BigDecimal probability = collisionProbability(m, w, l);
        int step = 1;
        // binary search
        boolean doubling = true;
        while (probability.compareTo(BigDecimalUtils.STATS_NEG_PROG) > 0 || step > 1) {
            if (probability.compareTo(BigDecimalUtils.STATS_NEG_PROG) > 0) {
                // we need to increase l
                if (doubling) {
                    step = Math.max(1, step * 2);
                } else {
                    step = Math.max(1, step / 2);
                }
                l += step;
            } else {
                // we need to slowly decrease l
                doubling = false;
                step = Math.max(1, step / 2);
                l -= step;
            }
            probability = collisionProbability(m, w, l);
        }
        return l;
    }

    /**
     * Gets collision probability when putting m elements into w bins.
     *
     * @param m m elements.
     * @param w w bins.
     * @param l l fingerprint bit length.
     * @return collision probability.
     */
    private static BigDecimal collisionProbability(int m, int w, int l) {
        assert m > 0 && w > 0 && l > 0;
        // M / w
        int exp = (int) Math.ceil((double) m / w);
        // (1 - 2^{-l})^{M / w}
        return BigDecimal.ONE.subtract(
            BigDecimal.ONE.subtract(BigDecimalUtils.HALF.setScale(BigDecimalUtils.PRECISION, RoundingMode.HALF_UP).pow(l)).pow(exp)
        );
    }
}
