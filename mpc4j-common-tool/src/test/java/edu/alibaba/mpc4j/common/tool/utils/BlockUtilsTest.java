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
    public void testXor() {
        for (int round = 0; round < ROUND; round++) {
            byte[] x = BytesUtils.randomByteArray(BlockUtils.BYTE_LENGTH, secureRandom);
            byte[] y = BytesUtils.randomByteArray(BlockUtils.BYTE_LENGTH, secureRandom);
            byte[] copyX = BytesUtils.clone(x);
            byte[] copyY = BytesUtils.clone(y);
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
            byte[] x = BytesUtils.randomByteArray(BlockUtils.BYTE_LENGTH, secureRandom);
            byte[] y = BytesUtils.randomByteArray(BlockUtils.BYTE_LENGTH, secureRandom);
            byte[] copyY = BytesUtils.clone(y);
            byte[] expect = BytesUtils.clone(x);
            byte[] actual = BytesUtils.clone(x);
            BytesUtils.xori(expect, y);
            BlockUtils.xori(actual, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(y, copyY);
        }
    }

    @Test
    public void testAnd() {
        for (int round = 0; round < ROUND; round++) {
            byte[] x = BytesUtils.randomByteArray(BlockUtils.BYTE_LENGTH, secureRandom);
            byte[] y = BytesUtils.randomByteArray(BlockUtils.BYTE_LENGTH, secureRandom);
            byte[] copyX = BytesUtils.clone(x);
            byte[] copyY = BytesUtils.clone(y);
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
            byte[] x = BytesUtils.randomByteArray(BlockUtils.BYTE_LENGTH, secureRandom);
            byte[] y = BytesUtils.randomByteArray(BlockUtils.BYTE_LENGTH, secureRandom);
            byte[] copyY = BytesUtils.clone(y);
            byte[] expect = BytesUtils.clone(x);
            byte[] actual = BytesUtils.clone(x);
            BytesUtils.andi(expect, y);
            BlockUtils.andi(actual, y);
            Assert.assertArrayEquals(expect, actual);
            Assert.assertArrayEquals(y, copyY);
        }
    }
}
