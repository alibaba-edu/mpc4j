package edu.alibaba.mpc4j.common.tool.utils;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 大整数工具类测试。
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public class BigIntegerUtilsTest {
    /**
     * 随机测试轮数
     */
    private static final int RANDOM_ROUND = 100;
    /**
     * 上界
     */
    private static final long UPPER_BOUND = 100;
    /**
     * 默认随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testBigIntegerByteArray() {
        // 正数转换
        testBigIntegerByteArray(BigInteger.ONE, new byte[] {(byte)0x01,});
        testBigIntegerByteArray(BigInteger.valueOf(67), new byte[] {(byte)0x43,});
        testBigIntegerByteArray(BigInteger.valueOf(14915), new byte[] {(byte)0x3A, (byte)0x43,});
        testBigIntegerByteArray(
            BigIntegerUtils.INT_MAX_VALUE,
            new byte[] {(byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF,}
        );
        testBigIntegerByteArray(
            BigIntegerUtils.LONG_MAX_VALUE,
            new byte[] {(byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,}
        );
        // 0转换
        testBigIntegerByteArray(BigInteger.ZERO, new byte[] {(byte)0x00,});
        // 负数转换
        testBigIntegerByteArray(BigInteger.ONE.negate(), new byte[] {(byte)0xFF,});
        testBigIntegerByteArray(
            BigIntegerUtils.INT_MIN_VALUE,
            new byte[] {(byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00,}
        );
        testBigIntegerByteArray(
            BigIntegerUtils.LONG_MIN_VALUE,
            new byte[] {(byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,}
        );
    }

    private void testBigIntegerByteArray(BigInteger bigInteger, byte[] byteArray) {
        byte[] convertByteArray = BigIntegerUtils.bigIntegerToByteArray(bigInteger);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        BigInteger convertBigInteger = BigIntegerUtils.byteArrayToBigInteger(byteArray);
        Assert.assertEquals(bigInteger, convertBigInteger);
    }

    @Test
    public void testNonNegBigIntegerByteArray() {
        // 0转换
        testNonNegBigIntegerByteArray(BigInteger.ZERO, new byte[] {(byte)0x00,});
        testNonNegBigIntegerByteArray(BigInteger.ZERO, new byte[] {(byte)0x00, (byte)0x00,});
        // 正数转换
        testNonNegBigIntegerByteArray(BigInteger.ONE, new byte[] {(byte)0x01,});
        testNonNegBigIntegerByteArray(BigInteger.ONE, new byte[] {(byte)0x00, (byte)0x01,});
        // 正好需要符号位的正数转换
        testNonNegBigIntegerByteArray(BigInteger.valueOf(255), new byte[] {(byte)0xFF,});
        testNonNegBigIntegerByteArray(BigInteger.valueOf(255), new byte[] {(byte)0x00, (byte)0xFF,});
    }

    private void testNonNegBigIntegerByteArray(BigInteger nonNegBigInteger, byte[] byteArray) {
        byte[] convertByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(nonNegBigInteger, byteArray.length);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        BigInteger convertNonNegBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(byteArray);
        Assert.assertEquals(nonNegBigInteger, convertNonNegBigInteger);
    }

    @Test
    public void testInvalidBinomial() {
        // try to compute C(-1, 0)
        Assert.assertThrows(IllegalArgumentException.class, () -> BigIntegerUtils.binomial(-1, 0));
        // try to compute C(10, -1)
        Assert.assertThrows(IllegalArgumentException.class, () -> BigIntegerUtils.binomial(10, -1));
        // try to compute C(10, 11)
        Assert.assertThrows(IllegalArgumentException.class, () -> BigIntegerUtils.binomial(10, 11));
    }

    @Test
    public void testBinomial() {
        // C(1, 0) = 1
        testBinomial(1, 0, BigInteger.ONE);
        // C(1, 1) = 1
        testBinomial(1, 1, BigInteger.ONE);
        // C(10, 0) = 1
        testBinomial(10, 0, BigInteger.ONE);
        // C(10, 1) = 10
        testBinomial(10, 1, BigInteger.valueOf(10));
        // C(10, 9) = 10
        testBinomial(10, 9, BigInteger.valueOf(10));
        // C(10, 10) = 1
        testBinomial(10, 10, BigInteger.ONE);
        // C(10, 5) = 252
        testBinomial(10, 5, BigInteger.valueOf(252));
        // C(10, 3) = 120
        testBinomial(10, 3, BigInteger.valueOf(120));
        // C(10, 6) = 210
        testBinomial(10, 6, BigInteger.valueOf(210));
    }

    private void testBinomial(int n, int m, BigInteger truth) {
        BigInteger combinatorial = BigIntegerUtils.binomial(n, m);
        Assert.assertEquals(truth, combinatorial);
    }

    @Test
    public void testSign() {
        // 1的验证结果
        Assert.assertTrue(BigIntegerUtils.positive(BigInteger.ONE));
        Assert.assertTrue(BigIntegerUtils.nonNegative(BigInteger.ONE));
        Assert.assertFalse(BigIntegerUtils.negative(BigInteger.ONE));
        Assert.assertFalse(BigIntegerUtils.nonPositive(BigInteger.ONE));
        // 0的验证结果
        Assert.assertTrue(BigIntegerUtils.nonNegative(BigInteger.ZERO));
        Assert.assertTrue(BigIntegerUtils.nonPositive(BigInteger.ZERO));
        // -1的验证结果
        Assert.assertTrue(BigIntegerUtils.negative(BigInteger.ONE.negate()));
        Assert.assertTrue(BigIntegerUtils.nonPositive(BigInteger.ONE.negate()));
        Assert.assertFalse(BigIntegerUtils.positive(BigInteger.ONE.negate()));
        Assert.assertFalse(BigIntegerUtils.nonNegative(BigInteger.ONE.negate()));
    }

    @Test
    public void testComparison() {
        // 1和-1、1的比较结果
        Assert.assertTrue(BigIntegerUtils.greater(BigInteger.ONE, BigInteger.ONE.negate()));
        Assert.assertTrue(BigIntegerUtils.greaterOrEqual(BigInteger.ONE, BigInteger.ONE.negate()));
        Assert.assertTrue(BigIntegerUtils.greaterOrEqual(BigInteger.ONE, BigInteger.ONE));
        // -1和1、-1的比较结果
        Assert.assertTrue(BigIntegerUtils.less(BigInteger.ONE.negate(), BigInteger.ONE));
        Assert.assertTrue(BigIntegerUtils.lessOrEqual(BigInteger.ONE.negate(), BigInteger.ONE));
        Assert.assertTrue(BigIntegerUtils.lessOrEqual(BigInteger.ONE.negate(), BigInteger.ONE.negate()));
        // -1和1的比较结果
        Assert.assertFalse(BigIntegerUtils.greater(BigInteger.ONE.negate(), BigInteger.ONE));
        Assert.assertFalse(BigIntegerUtils.greaterOrEqual(BigInteger.ONE.negate(), BigInteger.ONE));
        // 1和-1的比较结果
        Assert.assertFalse(BigIntegerUtils.less(BigInteger.ONE, BigInteger.ONE.negate()));
        Assert.assertFalse(BigIntegerUtils.lessOrEqual(BigInteger.ONE, BigInteger.ONE.negate()));
    }

    @Test
    public void testSqrtFloor() {
        // √0 = 0
        Assert.assertEquals(BigInteger.ZERO, BigIntegerUtils.sqrtFloor(BigInteger.ZERO));
        // √0 = 1
        Assert.assertEquals(BigInteger.ONE, BigIntegerUtils.sqrtFloor(BigInteger.ONE));
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // random picks an element in [1, 2^512), first square then square root.
            BigInteger n = BigIntegerUtils.randomPositive(BigInteger.ONE.shiftLeft(512), SECURE_RANDOM);
            BigInteger nSquared = n.multiply(n);
            Assert.assertEquals(n, BigIntegerUtils.sqrtFloor(nSquared));
        }
    }

    @Test
    public void testLog2() {
        for (int t = 0; t < Long.SIZE - 1; t++) {
            // x = 2^t
            BigInteger x = BigInteger.valueOf(1L << t);
            Assert.assertEquals(t, BigIntegerUtils.log2(x), DoubleUtils.PRECISION);
        }
    }

    @Test
    public void testIllegalRandomNonNegative() {
        // try to generate random non-negative with negative n
        Assert.assertThrows(IllegalArgumentException.class, () ->
            BigIntegerUtils.randomNonNegative(BigInteger.valueOf(-1), SECURE_RANDOM)
        );
        // generate random non-negative with n = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            BigIntegerUtils.randomNonNegative(BigInteger.ZERO, SECURE_RANDOM)
        );
    }

    @Test
    public void testRandomNonNegative() {
        for (int bound = 1; bound < UPPER_BOUND; bound++) {
            for (int round = 0; round < RANDOM_ROUND; round++) {
                int random = BigIntegerUtils.randomNonNegative(BigInteger.valueOf(bound), SECURE_RANDOM).intValue();
                Assert.assertTrue(random >= 0 && random < bound);
            }
        }
    }

    @Test
    public void testIllegalRandomPositive() {
        // try to generate random positive with negative n
        Assert.assertThrows(IllegalArgumentException.class, () ->
            BigIntegerUtils.randomPositive(BigInteger.valueOf(-1), SECURE_RANDOM)
        );
        // try to generate random positive with n = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            BigIntegerUtils.randomPositive(BigInteger.ZERO, SECURE_RANDOM)
        );
        // try to generate random positive with n = 1
        Assert.assertThrows(IllegalArgumentException.class, () ->
            BigIntegerUtils.randomPositive(BigInteger.ONE, SECURE_RANDOM)
        );
    }

    @Test
    public void testRandomPositive() {
        for (int bound = 2; bound < UPPER_BOUND; bound++) {
            for (int round = 0; round < RANDOM_ROUND; round++) {
                int random = BigIntegerUtils.randomPositive(BigInteger.valueOf(bound), SECURE_RANDOM).intValue();
                Assert.assertTrue(random > 0 && random < bound);
            }
        }
    }
}
