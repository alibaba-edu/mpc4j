package edu.alibaba.mpc4j.crypto.algs.utils.range;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * BigInteger Range tests.
 *
 * @author Weiran Liu
 * @date 2024/5/14
 */
public class BigIntegerRangeTest {

    @Test
    public void testIllegalArguments() {
        // [-1, -2]
        Assert.assertThrows(IllegalArgumentException.class, () -> new BigIntegerRange(-1, -2));
        // [0, -1]
        Assert.assertThrows(IllegalArgumentException.class, () -> new BigIntegerRange(0, -1));
        // [1, 0]
        Assert.assertThrows(IllegalArgumentException.class, () -> new BigIntegerRange(1, 0));
        // [2, 1]
        Assert.assertThrows(IllegalArgumentException.class, () -> new BigIntegerRange(2, 1));
        // [2, -2]
        Assert.assertThrows(IllegalArgumentException.class, () -> new BigIntegerRange(2, -2));
        // [-2, 2], and set range
        BigIntegerRange range = new BigIntegerRange(-2, 2);
        Assert.assertThrows(IllegalArgumentException.class, () -> range.setStart(3));
        Assert.assertThrows(IllegalArgumentException.class, () -> range.setEnd(-3));
    }

    @Test
    public void testRange() {
        // [0, 0]
        BigIntegerRange range = new BigIntegerRange(0, 0);
        Assert.assertEquals(BigInteger.valueOf(1), range.size());
        Assert.assertFalse(range.contains(-1));
        Assert.assertTrue(range.contains(0));
        Assert.assertFalse(range.contains(1));
        // [-2, 2]
        range = new BigIntegerRange(-2, 2);
        Assert.assertEquals(5, range.size().longValue());
        Assert.assertFalse(range.contains(range.getStart().longValue() - 1));
        for (long value = range.getStart().longValue(); value <= range.getEnd().longValue(); value++) {
            Assert.assertTrue(range.contains(value));
        }
        Assert.assertFalse(range.contains(range.getEnd().longValue() + 1));
    }
}
