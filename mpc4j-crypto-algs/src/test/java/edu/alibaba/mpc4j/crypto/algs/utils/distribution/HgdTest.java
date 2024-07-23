package edu.alibaba.mpc4j.crypto.algs.utils.distribution;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.HgdFactory.HgdType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Long Hypergeometric distribution unit test.
 *
 * @author Weiran Liu
 * @date 2024/1/7
 */
@RunWith(Parameterized.class)
public class HgdTest {
    /**
     * HGD
     */
    private final Hgd hgd;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RandomHGD
        configurations.add(new Object[]{HgdType.RANDOM.name()});
        // LongHGD
        configurations.add(new Object[]{HgdType.FAST.name()});
        // PreciseHGD
        configurations.add(new Object[]{HgdType.PRECISE.name()});

        return configurations;
    }

    public HgdTest(String name) {
        HgdType hgdType = HgdType.valueOf(name);
        hgd = HgdFactory.getInstance(hgdType);
        Assert.assertEquals(hgdType, hgd.getType());
    }

    @Test
    public void testConstant() {
        Coins coins = new Coins(new byte[CommonConstants.BLOCK_BYTE_LENGTH], new byte[CommonConstants.BLOCK_BYTE_LENGTH]);

        // no good items, r should be all 0
        {
            long n1, n2, n, k, r;
            // case n2 > 10
            n1 = 0L;
            n2 = 20L;
            n = n1 + n2;
            for (k = 0; k <= n; k++) {
                r = hgd.sample(k, n1, n2, coins);
                Assert.assertEquals(0, r);
            }
            // case n2 <= 10
            n2 = 5L;
            n = n1 + n2;
            for (k = 0; k <= n; k++) {
                r = hgd.sample(k, n1, n2, coins);
                Assert.assertEquals(0, r);
            }
        }

        // all good items, r should be all k
        {
            long n1, n2, n, k, r;
            // case n1 > 10
            n1 = 20L;
            n2 = 0L;
            n = n1 + n2;
            for (k = 0; k <= n; k++) {
                r = hgd.sample(k, n1, n2, coins);
                Assert.assertEquals(k, r);
            }
            // case n1 <= 10
            n1 = 5L;
            n = n1 + n2;
            for (k = 0; k <= n; k++) {
                r = hgd.sample(k, n1, n2, coins);
                Assert.assertEquals(k, r);
            }
        }
        // r should be 0 when k is 0, r should be n1 when k is n
        {
            // case: n1 > 10, n2 > 10
            long n1, n2, n, k, r;
            n1 = 20L;
            n2 = 20L;
            n = n1 + n2;
            k = 0L;
            r = hgd.sample(k, n1, n2, coins);
            Assert.assertEquals(0L, r);
            k = n;
            r = hgd.sample(k, n1, n2, coins);
            Assert.assertEquals(n1, r);
            // case: n1 <= 10, n2 > 10
            n1 = 5L;
            // n2 = 20L;
            n = n1 + n2;
            k = 0L;
            r = hgd.sample(k, n1, n2, coins);
            Assert.assertEquals(0, r);
            k = n;
            r = hgd.sample(k, n1, n2, coins);
            Assert.assertEquals(n1, r);
            // case: n1 > 10, n2 <= 10
            n1 = 20L;
            n2 = 5L;
            // n = n1 + n2;
            k = 0L;
            r = hgd.sample(k, n1, n2, coins);
            Assert.assertEquals(0L, r);
            k = n;
            r = hgd.sample(k, n1, n2, coins);
            Assert.assertEquals(n1, r);
            // case: n1 <= 10, n2 <= 10
            n1 = 5L;
            // n2 = 5L;
            n = n1 + n2;
            k = 0L;
            r = hgd.sample(k, n1, n2, coins);
            Assert.assertEquals(0L, r);
            k = n;
            r = hgd.sample(k, n1, n2, coins);
            Assert.assertEquals(n1, r);
        }
    }

    @Test
    public void testRandom() {
        SecureRandom secureRandom = new SecureRandom();
        int totalRound = 100;
        for (int round = 0; round < totalRound; round++) {
            byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            secureRandom.nextBytes(key);
            byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            secureRandom.nextBytes(seed);
            Coins coins = new Coins(key, seed);

            // all r should be in range 0 <= r <= n1, 0 <= r <= k
            long n1, n2, n, k, r;
            // case: n1 > 10, n2 > 10
            n1 = 20L;
            n2 = 20L;
            n = n1 + n2;
            for (k = 0L; k <= n; k++) {
                r = hgd.sample(k, n1, n2, coins);
                Assert.assertTrue(r >= 0);
                Assert.assertTrue(r <= n1);
                Assert.assertTrue(r <= k);
            }
            // case: n1 <= 10, n2 > 10
            n1 = 5L;
            // n2 = 20L;
            n = n1 + n2;
            for (k = 0L; k <= n; k++) {
                r = hgd.sample(k, n1, n2, coins);
                Assert.assertTrue(r >= 0);
                Assert.assertTrue(r <= n1);
                Assert.assertTrue(r <= k);
            }
            // case: n1 > 10, n2 <= 10
            n1 = 20L;
            n2 = 5L;
            n = n1 + n2;
            for (k = 0L; k <= n; k++) {
                r = hgd.sample(k, n1, n2, coins);
                Assert.assertTrue(r >= 0);
                Assert.assertTrue(r <= n1);
                Assert.assertTrue(r <= k);
            }
            // case: n1 <= 10, n2 <= 10
            n1 = 5L;
            // n2 = 5L;
            n = n1 + n2;
            for (k = 0L; k <= n; k++) {
                r = hgd.sample(k, n1, n2, coins);
                Assert.assertTrue(r >= 0);
                Assert.assertTrue(r <= n1);
                Assert.assertTrue(r <= k);
            }
        }
    }
}
