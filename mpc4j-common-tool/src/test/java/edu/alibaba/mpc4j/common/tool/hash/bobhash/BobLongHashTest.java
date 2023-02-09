package edu.alibaba.mpc4j.common.tool.hash.bobhash;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * tests for BobLongHash.
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class BobLongHashTest {
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
        Assert.assertThrows(IllegalArgumentException.class, () -> new BobLongHash(-1));
        // try large prime index
        Assert.assertThrows(IllegalArgumentException.class, () -> new BobLongHash(BobHashUtils.PRIME_BIT_TABLE_SIZE));
    }

    @Test
    public void testNonDefaultPrimeIndex() {
        // choose some prime index
        testNonDefaultPrimeIndex(1);
        testNonDefaultPrimeIndex(BobHashUtils.PRIME_BIT_TABLE_SIZE - 1);
    }

    private void testNonDefaultPrimeIndex(int primeIndex) {
        BobLongHash bobLongHash = new BobLongHash(primeIndex);
        Set<Long> hashSet = IntStream.range(0, TEST_ROUND)
            .mapToLong(index -> bobLongHash.hash(LongUtils.longToByteArray(index)))
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
        BobLongHash bobLongHash = new BobLongHash(primeIndex);
        IntStream.range(0, TEST_ROUND).forEach(index -> {
            byte[] data = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(data);
            long hash1 = bobLongHash.hash(data);
            long hash2 = bobLongHash.hash(data);
            Assert.assertEquals(hash1, hash2);
        });
    }
}
