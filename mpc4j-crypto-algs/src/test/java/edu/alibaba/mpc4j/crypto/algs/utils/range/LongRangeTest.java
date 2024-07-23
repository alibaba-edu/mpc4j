package edu.alibaba.mpc4j.crypto.algs.utils.range;

import org.junit.Assert;
import org.junit.Test;

/**
 * Long Range test.
 *
 * @author Weiran Liu
 * @date 2024/5/14
 */
public class LongRangeTest {

    @Test
    public void testIllegalArguments() {
        // [-1, -2]
        Assert.assertThrows(IllegalArgumentException.class, () -> new LongRange(-1, -2));
        // [0, -1]
        Assert.assertThrows(IllegalArgumentException.class, () -> new LongRange(0, -1));
        // [1, 0]
        Assert.assertThrows(IllegalArgumentException.class, () -> new LongRange(1, 0));
        // [2, 1]
        Assert.assertThrows(IllegalArgumentException.class, () -> new LongRange(2, 1));
        // [2, -2]
        Assert.assertThrows(IllegalArgumentException.class, () -> new LongRange(2, -2));
        // [-2, 2], and set range
        LongRange range = new LongRange(-2, 2);
        Assert.assertThrows(IllegalArgumentException.class, () -> range.setStart(3));
        Assert.assertThrows(IllegalArgumentException.class, () -> range.setEnd(-3));
    }

    @Test
    public void testRange() {
        // [0, 0]
        LongRange range = new LongRange(0, 0);
        Assert.assertEquals(1, range.size());
        Assert.assertFalse(range.contains(-1));
        Assert.assertTrue(range.contains(0));
        Assert.assertFalse(range.contains(1));
        // [-2, 2]
        range = new LongRange(-2, 2);
        Assert.assertEquals(5, range.size());
        Assert.assertFalse(range.contains(range.getStart() - 1));
        for (long value = range.getStart(); value <= range.getEnd(); value++) {
            Assert.assertTrue(range.contains(value));
        }
        Assert.assertFalse(range.contains(range.getEnd() + 1));
    }
}
