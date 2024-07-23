package edu.alibaba.mpc4j.crypto.algs.utils.distribution;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.HgdFactory.HgdType;

/**
 * Random Hypergeometric distribution. The result does not satisfy the standard HGD, bug a totally random result.
 *
 * @author Weiran Liu
 * @date 2024/5/17
 */
public class RandomHgd implements Hgd {
    /**
     * singleton mode
     */
    private static final RandomHgd INSTANCE = new RandomHgd();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    static RandomHgd getInstance() {
        return INSTANCE;
    }

    /**
     * private constructor.
     */
    private RandomHgd() {
        // empty
    }

    @Override
    public HgdType getType() {
        return HgdType.RANDOM;
    }

    @Override
    public long sample(long k, long n1, long n2, Coins coins) {
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

        // when k <= n_1, we have that r <= k
        long maxR = Math.min(k, n1);
        // when k >= n_2, we have that r >= k - n_2
        long minR = Math.max(0, k - n2);
        // randomly sample a long
        return Math.abs(coins.nextLong() % (maxR - minR + 1)) + minR;
    }
}
