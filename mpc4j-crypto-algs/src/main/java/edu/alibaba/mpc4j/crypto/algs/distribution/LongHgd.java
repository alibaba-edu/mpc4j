package edu.alibaba.mpc4j.crypto.algs.distribution;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.math3.util.Precision;

/**
 * Hypergeometric distribution, where the inputs and the output are represented using long.
 * <p></p>
 * The hypergeometric distribution applies to a finite population of size n = n_1 + n_2, n_1 of which have a particular
 * attribute and n_2 of which do not. When a random sample of size k is drawn from such population, the number of items
 * with the attribute is hypergeometrically distributed with parameters n_1, n_2, and k.
 *
 * @author Weiran Liu
 * @date 2024/1/7
 */
@SuppressWarnings("AlibabaUndefineMagicConstant")
public class LongHgd {
    /**
     * precision
     */
    private final static double PRECISION = 1e-23;
    /**
     * 2π
     */
    private final static double DOUBLE_2_PI = 2 * Math.PI;
    /**
     * 0.5 * log(2π)
     */
    private final static double DOUBLE_HALF_LOG_2_PI = 0.5 * Math.log(DOUBLE_2_PI);

    /**
     * Samples from the hypergeometric distribution.
     *
     * @param k     sample k of the items.
     * @param n1    n_1 of the items have a particular attribute (good).
     * @param n2    n_2 of the items do not have a particular attribute (bad).
     * @param coins random coins.
     * @return number of items that has the particular attribute.
     */
    public static long sample(long k, long n1, long n2, Coins coins) {
        // n_1 >= 0
        MathPreconditions.checkNonNegative("n1", n1);
        // n_2 >= 0
        MathPreconditions.checkNonNegative("n2", n2);
        long n = n1 + n2;
        // n = n_1 + n_2 > 0
        MathPreconditions.checkPositive("n", n);
        // 0 <= k <= n
        MathPreconditions.checkNonNegativeInRangeClosed("k", k, n);

        // special case: k = 0, r = 0
        if (k == 0L) {
            return 0L;
        }
        // special case: n1 = 0, r = 0
        if (n1 == 0L) {
            return 0L;
        }
        // special case: n2 = 0, r = k
        if (n2 == 0L) {
            return k;
        }
        // special case: k = n, r = n1
        if (k == n) {
            return n1;
        }

        if (k > 10) {
            return hypergeometricHrua(coins, n1, n2, k);
        } else {
            return hypergeometricHyp(coins, n1, n2, k);
        }
    }

    private static long hypergeometricHyp(Coins coins, long good, long bad, long sample) {
        double d1 = (double) (bad + good - sample);
        double d2 = (double) Math.min(bad, good);

        double y = d2;
        double k = (double) sample;

        while (y > 0) {
            double u = coins.nextFloat();

            double d1K = d1 + k;
            double inner = Math.floor(u + y / d1K);
            y = y - inner;

            k = k - 1;
            if (Precision.equals(k, 0, PRECISION)) {
                break;
            }
        }

        long z = (long) (d2 - y);
        if (good > bad) {
            z = sample - z;
        }

        return z;
    }

    /**
     * parameter D1 for HRUA algorithm
     */
    private final static double D1 = 1.7155277699214135;
    /**
     * parameter D2 for HRUA algorithm
     */
    private final static double D2 = 0.8989161620588988;

    private static long hypergeometricHrua(Coins coins, long good, long bad, long k) {
        boolean moreGood;
        double badBound = (double) bad;
        double goodBound = (double) good;
        double mingoodbad;
        double maxgoodbad;
        if (good > bad) {
            moreGood = true;
            mingoodbad = badBound;
            maxgoodbad = goodBound;
        } else {
            moreGood = false;
            mingoodbad = goodBound;
            maxgoodbad = badBound;
        }

        double popSize = good + bad;
        double sample = (double) k;
        double m = Math.min(sample, popSize - sample);
        double d4 = mingoodbad / popSize;
        double d5 = 1.0 - d4;
        double d6 = m * d4 + 0.5;

        double d7a = (popSize - m) * sample * d4 * d5 / (popSize - 1) + 0.5;
        double d7 = Math.sqrt(d7a);

        double d8 = D1 * d7 + D2;

        double minGoodBadPlus1 = mingoodbad + 1;
        double d9 = (m + 1) * minGoodBadPlus1 / (popSize + 2);

        double d9plus1 = d9 + 1;
        double d10 = loggam(d9plus1) + loggam(minGoodBadPlus1 - d9)
            + loggam(m - d9 + 1) + loggam(maxgoodbad - m + d9plus1);

        double d11a = Math.min(m, mingoodbad) + 1;
        double d11b = Math.floor(d6 + (d7 * 16));
        double d11 = Math.min(d11a, d11b);

        double z;
        while (true) {
            double x = coins.nextFloat();
            double y = coins.nextFloat();
            double w = d6 + d8 * (y - 0.5) / x;

            if (w < 0 || w >= d11) {
                continue;
            }

            z = Math.floor(w);

            double zPlus1 = z + 1;
            double zMinus1 = z - 1;
            double t = d10 - (loggam(zPlus1) + (loggam(mingoodbad - (zMinus1)))
                + loggam(m - zMinus1) + loggam(maxgoodbad - m + zPlus1));

            if (x * (4 - x) - 3 <= t) {
                break;
            }
            if (x * (x - t) >= 1) {
                continue;
            }
            if (2 * Math.log(x) <= t) {
                break;
            }
        }

        if (moreGood) {
            z = m - z;
        }
        if (m < sample) {
            z = goodBound - z;
        }

        return (long) z;
    }

    /**
     * parameter A
     */
    private final static double[] A = new double[]{
        8.333333333333333e-02, -2.777777777777778e-03,
        7.936507936507937e-04, -5.952380952380952e-04,
        8.417508417508418e-04, -1.917526917526918e-03,
        6.410256410256410e-03, -2.955065359477124e-02,
        1.796443723688307e-01, -1.39243221690590e+00,
    };

    private static double loggam(double x) {
        double x0 = x;
        int n = 0;

        if (Precision.equals(x, 1, 1e-23) || Precision.equals(x, 2, DoubleUtils.PRECISION)) {
            return 0.0;
        } else if (x <= 7.0) {
            n = (int) (7.0 - x);
            x0 = x + n;
        }

        double x2 = 1.0 / (x0 * x0);

        double gl0 = A[9];

        for (int k = 8; k >= 0; k--) {
            gl0 = gl0 * x2;
            gl0 = gl0 + A[k];
        }

        double gl = gl0 / x0 + DOUBLE_HALF_LOG_2_PI + (x0 - 0.5) * Math.log(x0) - x0;

        if (x <= 7.0) {
            for (int k = 1; k <= n + 1; k++) {
                x0 = x0 - 1;
                gl = gl - Math.log(x0);
            }
        }

        return gl;
    }
}
