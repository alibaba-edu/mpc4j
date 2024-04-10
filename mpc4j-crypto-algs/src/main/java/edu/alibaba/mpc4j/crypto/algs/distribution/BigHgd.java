package edu.alibaba.mpc4j.crypto.algs.distribution;

import ch.obermuhlner.math.big.BigDecimalMath;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Hypergeometric distribution, where the inputs and the output are represented using BigInteger.
 * <p></p>
 * The implementation is modified from https://github.com/ssavvides/jope/blob/master/src/jope/Hgd.java.
 * <p></p>
 * The hypergeometric distribution applies to a finite population of size n = n_1 + n_2, n_1 of which have a particular
 * attribute and n_2 of which do not. When a random sample of size k is drawn from such population, the number of items
 * with the attribute is hypergeometrically distributed with parameters n_1, n_2, and k.
 *
 * @author Weiran Liu
 * @date 2024/1/7
 */
@SuppressWarnings("AlibabaUndefineMagicConstant")
public class BigHgd {
    /**
     * 0.5
     */
    private final static BigDecimal DEC_HALF = BigDecimalUtils.HALF;
    /**
     * 2
     */
    private final static BigDecimal DEC_2 = BigDecimal.valueOf(2);
    /**
     * 3
     */
    private final static BigDecimal DEC_3 = BigDecimal.valueOf(3);
    /**
     * 4
     */
    private final static BigDecimal DEC_4 = BigDecimal.valueOf(4);
    /**
     * 7
     */
    private final static BigDecimal DEC_7 = BigDecimal.valueOf(7);
    /**
     * 16
     */
    private final static BigDecimal DEC_16 = BigDecimal.valueOf(16);
    /**
     * 2π
     */
    private final static BigDecimal DEC_2_PI = BigDecimal.valueOf(2 * Math.PI);
    /**
     * 0.5 * log(2π)
     */
    private final static BigDecimal DEC_HALF_LOG_2_PI = DEC_HALF.multiply(BigDecimalMath.log(DEC_2_PI, BigDecimalUtils.MATH_CONTEXT));
    /**
     * precision used in OPE
     */
    private static final int OPE_PRECISION = 10;
    /**
     * MathContext used in OPE
     */
    private static final MathContext OPE_MATH_CONTEXT = new MathContext(10);
    /**
     * RoundingMode used in OPE
     */
    private static final RoundingMode OPE_RM = RoundingMode.HALF_UP;

    /**
     * Samples from the hypergeometric distribution.
     *
     * @param k     sample k of the items.
     * @param n1    n_1 of the items have a particular attribute (good).
     * @param n2    n_2 of the items do not have a particular attribute (bad).
     * @param coins random coins.
     * @return number of items that has the particular attribute.
     */
    public static BigInteger sample(BigInteger k, BigInteger n1, BigInteger n2, Coins coins) {
        // n_1 >= 0
        MathPreconditions.checkNonNegative("n1", n1);
        // n_2 >= 0
        MathPreconditions.checkNonNegative("n2", n2);
        BigInteger n = n1.add(n2);
        // n = n_1 + n_2 > 0
        MathPreconditions.checkPositive("n", n);
        // 0 <= k <= n
        MathPreconditions.checkNonNegativeInRangeClosed("k", k, n);

        // special case: k = 0, r = 0
        if (k.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        }
        // special case: n1 = 0, r = 0
        if (n1.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        }
        // special case: n2 = 0, r = k
        if (n2.equals(BigInteger.ZERO)) {
            return k;
        }
        // special case: k = n, r = n1
        if (k.equals(n)) {
            return n1;
        }

        if (k.compareTo(BigInteger.TEN) > 0) {
            return hypergeometricHrua(coins, n1, n2, k);
        } else {
            return hypergeometricHyp(coins, n1, n2, k);
        }
    }

    private static BigInteger hypergeometricHyp(Coins coins, BigInteger good, BigInteger bad, BigInteger k) {
        BigDecimal d1 = new BigDecimal(bad.add(good).subtract(k));
        BigDecimal d2 = new BigDecimal(bad.min(good));

        BigDecimal y = d2;
        BigDecimal copyK = new BigDecimal(k);

        while (y.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal u = BigDecimal.valueOf(coins.nextFloat());

            BigDecimal d1K = d1.add(copyK);
            BigDecimal inner = u.add(y.divide(d1K, OPE_PRECISION, OPE_RM)).setScale(0, RoundingMode.FLOOR);
            y = y.subtract(inner);

            copyK = copyK.subtract(BigDecimal.ONE);
            if (copyK.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
        }

        BigInteger z = d2.subtract(y).toBigInteger();
        if (good.compareTo(bad) > 0) {
            z = k.subtract(z);
        }

        return z;
    }

    /**
     * parameter D1 for HRUA algorithm
     */
    private final static BigDecimal D1 = new BigDecimal("1.7155277699214135");
    /**
     * parameter D2 for HRUA algorithm
     */
    private final static BigDecimal D2 = new BigDecimal("0.8989161620588988");

    private static BigInteger hypergeometricHrua(Coins coins, BigInteger good, BigInteger bad, BigInteger k) {
        boolean moreGood;
        BigDecimal badBound = new BigDecimal(bad);
        BigDecimal goodBound = new BigDecimal(good);
        BigDecimal mingoodbad;
        BigDecimal maxgoodbad;
        if (good.compareTo(bad) > 0) {
            moreGood = true;
            mingoodbad = badBound;
            maxgoodbad = goodBound;
        } else {
            moreGood = false;
            mingoodbad = goodBound;
            maxgoodbad = badBound;
        }

        BigDecimal popSize = new BigDecimal(good.add(bad));
        BigDecimal sample = new BigDecimal(k);
        BigDecimal m = sample.min(popSize.subtract(sample));
        BigDecimal d4 = mingoodbad.divide(popSize, OPE_PRECISION, OPE_RM);
        BigDecimal d5 = BigDecimal.ONE.subtract(d4);
        BigDecimal d6 = m.multiply(d4).add(DEC_HALF);

        BigDecimal d7a = popSize.subtract(m).multiply(sample).multiply(d4).multiply(d5)
            .divide(popSize.subtract(BigDecimal.ONE), OPE_PRECISION, OPE_RM).add(DEC_HALF);
        BigDecimal d7 = BigDecimalMath.sqrt(d7a, BigDecimalUtils.MATH_CONTEXT);

        BigDecimal d8 = D1.multiply(d7).add(D2);

        BigDecimal mingoodbadplus1 = mingoodbad.add(BigDecimal.ONE);
        BigDecimal d9 = m.add(BigDecimal.ONE).multiply(mingoodbadplus1).divide(popSize.add(DEC_2), OPE_PRECISION, OPE_RM);

        BigDecimal d9plus1 = d9.add(BigDecimal.ONE);
        BigDecimal d10 = loggam(d9plus1).add(loggam(mingoodbadplus1.subtract(d9)))
            .add(loggam(m.subtract(d9).add(BigDecimal.ONE)))
            .add(loggam(maxgoodbad.subtract(m).add(d9plus1)));

        BigDecimal d11a = m.min(mingoodbad).add(BigDecimal.ONE);
        BigDecimal d11b = d6.add(d7.multiply(DEC_16)).setScale(0, RoundingMode.FLOOR);
        BigDecimal d11 = d11a.min(d11b);

        BigDecimal z;
        while (true) {
            BigDecimal x = BigDecimal.valueOf(coins.nextFloat());
            BigDecimal y = BigDecimal.valueOf(coins.nextFloat());
            BigDecimal w = d6.add(d8.multiply(y.subtract(DEC_HALF)).divide(x, OPE_PRECISION, OPE_RM));

            if (w.compareTo(BigDecimal.ZERO) < 0 || w.compareTo(d11) >= 0) {
                continue;
            }

            z = w.setScale(0, RoundingMode.FLOOR);

            BigDecimal zPlus1 = z.add(BigDecimal.ONE);
            BigDecimal zMinus1 = z.subtract(BigDecimal.ONE);
            BigDecimal t = d10.subtract(loggam(zPlus1).add(loggam(mingoodbad.subtract(zMinus1)))
                .add(loggam(m.subtract(zMinus1)))
                .add(loggam(maxgoodbad.subtract(m).add(zPlus1))));

            if (x.multiply(DEC_4.subtract(x)).subtract(DEC_3).compareTo(t) <= 0) {
                break;
            }
            if (x.multiply(x.subtract(t)).compareTo(BigDecimal.ONE) >= 0) {
                continue;
            }
            if (DEC_2.multiply(BigDecimalMath.log(x, BigDecimalUtils.MATH_CONTEXT)).compareTo(t) <= 0) {
                break;
            }
        }

        if (moreGood) {
            z = m.subtract(z);
        }
        if (m.compareTo(sample) < 0) {
            z = goodBound.subtract(z);
        }

        return z.toBigInteger();
    }

    /**
     * parameter A
     */
    private final static BigDecimal[] A = new BigDecimal[]{
        BigDecimal.valueOf(8.333333333333333e-02), BigDecimal.valueOf(-2.777777777777778e-03),
        BigDecimal.valueOf(7.936507936507937e-04), BigDecimal.valueOf(-5.952380952380952e-04),
        BigDecimal.valueOf(8.417508417508418e-04), BigDecimal.valueOf(-1.917526917526918e-03),
        BigDecimal.valueOf(6.410256410256410e-03), BigDecimal.valueOf(-2.955065359477124e-02),
        BigDecimal.valueOf(1.796443723688307e-01), BigDecimal.valueOf(-1.39243221690590e+00),
    };

    private static BigDecimal loggam(BigDecimal x) {
        BigDecimal x0 = x;
        int n = 0;

        if (x.compareTo(BigDecimal.ONE) == 0 || x.compareTo(DEC_2) == 0) {
            return BigDecimal.ZERO;
        } else if (x.compareTo(DEC_7) <= 0) {
            n = (int) (7.0 - x.doubleValue());
            x0 = x.add(new BigDecimal(n));
        }

        BigDecimal x2 = BigDecimal.ONE.divide(x0.multiply(x0), OPE_PRECISION, OPE_RM);

        BigDecimal gl0 = A[9];

        for (int k = 8; k >= 0; k--) {
            gl0 = gl0.multiply(x2);
            gl0 = gl0.add(A[k]);
        }

        BigDecimal gl = gl0.divide(x0, OPE_PRECISION, OPE_RM).add(DEC_HALF_LOG_2_PI)
            .add(x0.subtract(DEC_HALF).multiply(BigDecimalMath.log(x0, OPE_MATH_CONTEXT)))
            .subtract(x0);

        if (x.compareTo(DEC_7) <= 0) {
            for (int k = 1; k <= n + 1; k++) {
                x0 = x0.subtract(BigDecimal.ONE);
                gl = gl.subtract(BigDecimalMath.log(x0, OPE_MATH_CONTEXT));
            }
        }

        return gl;
    }
}
