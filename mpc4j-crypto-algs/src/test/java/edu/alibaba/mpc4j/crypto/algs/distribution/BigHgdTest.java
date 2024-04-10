package edu.alibaba.mpc4j.crypto.algs.distribution;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.algs.distribution.BigHgd;
import edu.alibaba.mpc4j.crypto.algs.distribution.Coins;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * BigInteger Hypergeometric distribution unit test.
 *
 * @author Weiran Liu
 * @date 2024/1/7
 */
public class BigHgdTest {

    @Test
    public void testConstant() {
        Coins coins = new Coins(new byte[CommonConstants.BLOCK_BYTE_LENGTH], new byte[CommonConstants.BLOCK_BYTE_LENGTH]);

        // no good items, r should be all 0
        {
            BigInteger n1, n2, n, k, r;
            // case n2 > 10
            n1 = BigInteger.ZERO;
            n2 = BigInteger.valueOf(20);
            n = n1.add(n2);
            for (k = BigInteger.ZERO; k.compareTo(n) <= 0; k = k.add(BigInteger.ONE)) {
                r = BigHgd.sample(k, n1, n2, coins);
                Assert.assertEquals(BigInteger.ZERO, r);
            }
            // case n2 <= 10
            n2 = BigInteger.valueOf(5);
            n = n1.add(n2);
            for (k = BigInteger.ZERO; k.compareTo(n) <= 0; k = k.add(BigInteger.ONE)) {
                r = BigHgd.sample(k, n1, n2, coins);
                Assert.assertEquals(BigInteger.ZERO, r);
            }
        }

        // all good items, r should be all k
        {
            BigInteger n1, n2, n, k, r;
            // case n1 > 10
            n1 = BigInteger.valueOf(20);
            n2 = BigInteger.ZERO;
            n = n1.add(n2);
            for (k = BigInteger.ZERO; k.compareTo(n) <= 0; k = k.add(BigInteger.ONE)) {
                r = BigHgd.sample(k, n1, n2, coins);
                Assert.assertEquals(k, r);
            }
            // case n1 <= 10
            n1 = BigInteger.valueOf(5);
            n = n1.add(n2);
            for (k = BigInteger.ZERO; k.compareTo(n) <= 0; k = k.add(BigInteger.ONE)) {
                r = BigHgd.sample(k, n1, n2, coins);
                Assert.assertEquals(k, r);
            }
        }
        // r should be 0 when k is 0, r should be n1 when k is n
        {
            // case: n1 > 10, n2 > 10
            BigInteger n1, n2, n, k, r;
            n1 = BigInteger.valueOf(20);
            n2 = BigInteger.valueOf(20);
            n = n1.add(n2);
            k = BigInteger.ZERO;
            r = BigHgd.sample(k, n1, n2, coins);
            Assert.assertEquals(BigInteger.ZERO, r);
            k = n;
            r = BigHgd.sample(k, n1, n2, coins);
            Assert.assertEquals(n1, r);
            // case: n1 <= 10, n2 > 10
            n1 = BigInteger.valueOf(5);
            n2 = BigInteger.valueOf(20);
            n = n1.add(n2);
            k = BigInteger.ZERO;
            r = BigHgd.sample(k, n1, n2, coins);
            Assert.assertEquals(BigInteger.ZERO, r);
            k = n;
            r = BigHgd.sample(k, n1, n2, coins);
            Assert.assertEquals(n1, r);
            // case: n1 > 10, n2 <= 10
            n1 = BigInteger.valueOf(20);
            n2 = BigInteger.valueOf(5);
            n = n1.add(n2);
            k = BigInteger.ZERO;
            r = BigHgd.sample(k, n1, n2, coins);
            Assert.assertEquals(BigInteger.ZERO, r);
            k = n;
            r = BigHgd.sample(k, n1, n2, coins);
            Assert.assertEquals(n1, r);
            // case: n1 <= 10, n2 <= 10
            n1 = BigInteger.valueOf(5);
            n2 = BigInteger.valueOf(5);
            n = n1.add(n2);
            k = BigInteger.ZERO;
            r = BigHgd.sample(k, n1, n2, coins);
            Assert.assertEquals(BigInteger.ZERO, r);
            k = n;
            r = BigHgd.sample(k, n1, n2, coins);
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
            BigInteger n1, n2, n, k, r;
            // case: n1 > 10, n2 > 10
            n1 = BigInteger.valueOf(20);
            n2 = BigInteger.valueOf(20);
            n = n1.add(n2);
            for (k = BigInteger.ZERO; k.compareTo(n) <= 0; k = k.add(BigInteger.ONE)) {
                r = BigHgd.sample(k, n1, n2, coins);
                Assert.assertTrue(r.compareTo(BigInteger.ZERO) >= 0);
                Assert.assertTrue(r.compareTo(n1) <= 0);
                Assert.assertTrue(r.compareTo(k) <= 0);
            }
            // case: n1 <= 10, n2 > 10
            n1 = BigInteger.valueOf(5);
            n2 = BigInteger.valueOf(20);
            n = n1.add(n2);
            for (k = BigInteger.ZERO; k.compareTo(n) <= 0; k = k.add(BigInteger.ONE)) {
                r = BigHgd.sample(k, n1, n2, coins);
                Assert.assertTrue(r.compareTo(BigInteger.ZERO) >= 0);
                Assert.assertTrue(r.compareTo(n1) <= 0);
                Assert.assertTrue(r.compareTo(k) <= 0);
            }
            // case: n1 > 10, n2 <= 10
            n1 = BigInteger.valueOf(20);
            n2 = BigInteger.valueOf(5);
            n = n1.add(n2);
            for (k = BigInteger.ZERO; k.compareTo(n) <= 0; k = k.add(BigInteger.ONE)) {
                r = BigHgd.sample(k, n1, n2, coins);
                Assert.assertTrue(r.compareTo(BigInteger.ZERO) >= 0);
                Assert.assertTrue(r.compareTo(n1) <= 0);
                Assert.assertTrue(r.compareTo(k) <= 0);
            }
            // case: n1 <= 10, n2 <= 10
            n1 = BigInteger.valueOf(5);
            n2 = BigInteger.valueOf(5);
            n = n1.add(n2);
            for (k = BigInteger.ZERO; k.compareTo(n) <= 0; k = k.add(BigInteger.ONE)) {
                r = BigHgd.sample(k, n1, n2, coins);
                Assert.assertTrue(r.compareTo(BigInteger.ZERO) >= 0);
                Assert.assertTrue(r.compareTo(n1) <= 0);
                Assert.assertTrue(r.compareTo(k) <= 0);
            }
        }
    }
}
