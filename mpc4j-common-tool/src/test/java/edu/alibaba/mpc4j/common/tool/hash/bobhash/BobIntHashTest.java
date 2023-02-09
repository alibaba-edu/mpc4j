package edu.alibaba.mpc4j.common.tool.hash.bobhash;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * tests for BobIntHash.
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class BobIntHashTest {
    /**
     * test round
     */
    private static final int TEST_ROUND = 1 << CommonConstants.STATS_BYTE_LENGTH;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testIllegalPrimeIndex() {
        // try negative prime index
        Assert.assertThrows(IllegalArgumentException.class, () -> new BobIntHash(-1));
        // try large prime index
        Assert.assertThrows(IllegalArgumentException.class, () -> new BobIntHash(BobHashUtils.PRIME_BIT_TABLE_SIZE));
    }

    @Test
    public void testNonDefaultPrimeIndex() {
        // choose some prime index
        testNonDefaultPrimeIndex(1);
        testNonDefaultPrimeIndex(BobHashUtils.PRIME_BIT_TABLE_SIZE - 1);
    }

    private void testNonDefaultPrimeIndex(int primeIndex) {
        BobIntHash bobIntHash = new BobIntHash(primeIndex);
        Set<Integer> hashSet = IntStream.range(0, TEST_ROUND)
            .map(index -> bobIntHash.hash(IntUtils.intToByteArray(index)))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(TEST_ROUND, hashSet.size());
    }

    @Test
    public void testNonDefaultPrimeIndexConsistency() {
        // choose some prime index
        testNonDefaultPrimeIndexConsistency(1);
        testNonDefaultPrimeIndexConsistency(BobHashUtils.PRIME_BIT_TABLE_SIZE - 1);
    }

    private void testNonDefaultPrimeIndexConsistency(int primeIndex) {
        BobIntHash bobIntHash = new BobIntHash(primeIndex);
        IntStream.range(0, TEST_ROUND).forEach(index -> {
            byte[] data = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(data);
            int hash1 = bobIntHash.hash(data);
            int hash2 = bobIntHash.hash(data);
            Assert.assertEquals(hash1, hash2);
        });
    }
}
