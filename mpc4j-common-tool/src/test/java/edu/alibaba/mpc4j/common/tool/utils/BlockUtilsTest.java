package edu.alibaba.mpc4j.common.tool.utils;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * tests for block utilities.
 *
 * @author Weiran Liu
 * @date 2025/1/8
 */
public class BlockUtilsTest {
    /**
     * round
     */
    private static final int ROUND = 1000;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public BlockUtilsTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testLongBlock() {
        for (int round = 0; round < ROUND; round++) {
            byte[] expect = BlockUtils.randomBlock(secureRandom);
            long[] actualLong = BlockUtils.toLongArray(expect);
            byte[] actual = BlockUtils.toByteArray(actualLong);
            Assert.assertArrayEquals(expect, actual);
        }
    }

    @Test
    public void testXor() {
        for (int round = 0; round < ROUND; round++) {
            byte[] x = BlockUtils.randomBlock(secureRandom);
            byte[] y = BlockUtils.randomBlock(secureRandom);
            byte[] copyX = BlockUtils.clone(x);
            byte[] copyY = BlockUtils.clone(y);
            byte[] expect = BytesUtils.xor(x, y);
            byte[] actual = BlockUtils.xor(x, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(x, copyX);
            Assert.assertArrayEquals(y, copyY);
        }
    }

    @Test
    public void testXori() {
        for (int round = 0; round < ROUND; round++) {
            byte[] x = BlockUtils.randomBlock(secureRandom);
            byte[] y = BlockUtils.randomBlock(secureRandom);
            byte[] copyY = BlockUtils.clone(y);
            byte[] expect = BlockUtils.clone(x);
            byte[] actual;
            BytesUtils.xori(expect, y);
            // naive
            actual = BlockUtils.clone(x);
            BlockUtils.naiveXori(actual, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(y, copyY);
            // unsafe
            actual = BlockUtils.clone(x);
            BlockUtils.unsafeXori(actual, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(y, copyY);
        }
    }

    @Test
    public void testAccumulateXori() {
        int num = 11;
        byte[][] xs = BlockUtils.randomBlocks(num, secureRandom);
        // naive
        byte[] expect = BlockUtils.zeroBlock();
        for (byte[] x : xs) {
            BlockUtils.xori(expect, x);
        }
        // unsafe
        long[] actualLong = BlockUtils.zeroLongBlock();
        for (byte[] x : xs) {
            BlockUtils.xori(actualLong, x);
        }
        byte[] actual = BlockUtils.toByteArray(actualLong);
        Assert.assertArrayEquals(expect, actual);
    }

    @Test
    public void testAnd() {
        for (int round = 0; round < ROUND; round++) {
            byte[] x = BlockUtils.randomBlock(secureRandom);
            byte[] y = BlockUtils.randomBlock(secureRandom);
            byte[] copyX = BlockUtils.clone(x);
            byte[] copyY = BlockUtils.clone(y);
            byte[] expect = BytesUtils.and(x, y);
            byte[] actual = BlockUtils.and(x, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(x, copyX);
            Assert.assertArrayEquals(y, copyY);
        }
    }

    @Test
    public void testAndi() {
        for (int round = 0; round < ROUND; round++) {
            byte[] x = BlockUtils.randomBlock(secureRandom);
            byte[] y = BlockUtils.randomBlock(secureRandom);
            byte[] copyY = BlockUtils.clone(y);
            byte[] expect = BlockUtils.clone(x);
            byte[] actual;
            BytesUtils.andi(expect, y);
            // naive
            actual = BlockUtils.clone(x);
            BlockUtils.naiveAndi(actual, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(y, copyY);
            // unsafe
            actual = BlockUtils.clone(x);
            BlockUtils.unsafeAndi(actual, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(y, copyY);
        }
    }

    @Test
    public void testOri() {
        for (int round = 0; round < ROUND; round++) {
            byte[] x = BlockUtils.randomBlock(secureRandom);
            byte[] y = BlockUtils.randomBlock(secureRandom);
            byte[] copyY = BlockUtils.clone(y);
            byte[] expect = BlockUtils.clone(x);
            byte[] actual;
            BytesUtils.ori(expect, y);
            // naive
            actual = BlockUtils.clone(x);
            BlockUtils.naiveOri(actual, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(y, copyY);
            // unsafe
            actual = BlockUtils.clone(x);
            BlockUtils.unsafeOri(actual, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(y, copyY);
        }
    }
}
