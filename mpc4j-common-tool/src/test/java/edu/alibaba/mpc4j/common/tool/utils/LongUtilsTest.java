package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * 长整数工具类测试。
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class LongUtilsTest {
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 随机测试轮数
     */
    private static final int RANDOM_ROUND = 100;
    /**
     * 上界
     */
    private static final long UPPER_BOUND = 100;

    @Test
    public void testInvalidLongByteArray() {
        try {
            // 尝试转换长度为0的字节数组
            LongUtils.byteArrayToLong(new byte[0]);
            throw new IllegalStateException("ERROR: successfully convert byte array with length 0 to long");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试转换Long.BYTES - 1长的字节数组
            LongUtils.byteArrayToLong(new byte[Long.BYTES - 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte array with length Long.BYTES - 1 to long"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试转换Long.BYTES + 1长的字节数组
            LongUtils.byteArrayToLong(new byte[Long.BYTES + 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte array with length Long.BYTES + 1 to long"
            );
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testLongByteArray() {
        testLongByteArray(
            0,
            new byte[] {
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            }
        );
        testLongByteArray(
            1,
            new byte[] {
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01,
            }
        );
        testLongByteArray(
            -1,
            new byte[] {
                (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            }
        );
        testLongByteArray(
            Long.MAX_VALUE,
            new byte[] {
                (byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            }
        );
        testLongByteArray(
            Long.MIN_VALUE,
            new byte[] {
                (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            }
        );
    }

    private void testLongByteArray(long value, byte[] byteArray) {
        byte[] convertByteArray = LongUtils.longToByteArray(value);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        long convertValue = LongUtils.byteArrayToLong(byteArray);
        Assert.assertEquals(value, convertValue);
    }

    @Test
    public void testInvalidLongArrayByteArray() {
        try {
            // 尝试将长度为0的long数组转换成字节数组
            LongUtils.longArrayToByteArray(new long[0]);
            throw new IllegalStateException("ERROR: successfully convert long[] with length 0 to byte array");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为Long.BYTES - 1的字节数组转换成long数组
            LongUtils.byteArrayToLongArray(new byte[Long.BYTES - 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length Long.BYTES - 1 to long array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为Long.BYTES + 1的字节数组转换成long数组
            LongUtils.byteArrayToLongArray(new byte[Long.BYTES + 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length Long.BYTES + 1 to long array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为2 * Long.BYTES - 1的字节数组转换成long数组
            LongUtils.byteArrayToLongArray(new byte[2 * Long.BYTES - 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length 2 * Long.BYTES - 1 to long array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为2 * Integer.BYTES + 1的字节数组转换成int数组
            LongUtils.byteArrayToLongArray(new byte[2 * Long.BYTES + 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length 2 * LongUtils.BYTES + 1 to long array"
            );
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testLongArrayByteArray() {
        testLongArrayByteArray(
            new long[] { 0x00 },
            new byte[] {
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            });
        testLongArrayByteArray(
            new long[] { Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE },
            new byte[] {
                (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01,
                (byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            }
        );
    }

    private void testLongArrayByteArray(long[] longArray, byte[] byteArray) {
        byte[] convertByteArray = LongUtils.longArrayToByteArray(longArray);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        long[] convertIntArray = LongUtils.byteArrayToLongArray(byteArray);
        Assert.assertArrayEquals(longArray, convertIntArray);
    }

    @Test
    public void testCeilLog2() {
        Assert.assertEquals(0, LongUtils.ceilLog2(1));
        // from 2^1 - 1 to 2^62 + 1
        for (int t = 2; t < Long.SIZE - 1; t++) {
            // x = 2^t
            long exactX = 1L << t;
            Assert.assertEquals(t, LongUtils.ceilLog2(exactX));
            long smallX = (1L << t) - 1;
            Assert.assertEquals(t, LongUtils.ceilLog2(smallX));
            long largeX = (1L << t) + 1;
            Assert.assertEquals(t + 1, LongUtils.ceilLog2(largeX));
            long primeX = ZpManager.getPrime(t - 1).longValue();
            Assert.assertEquals(t, LongUtils.ceilLog2(primeX));
        }
    }

    @Test
    public void testRandomNonNegative() {
        try {
            LongUtils.randomNonNegative(0, SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random non-negative with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            LongUtils.randomNonNegative(-1, SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random non-negative with negative n");
        } catch (AssertionError ignored) {

        }
        // 测试可以取到0，连续40次采样都为0的概率是1 / 2^40
        boolean success = false;
        for (int round = 0; round < CommonConstants.STATS_BIT_LENGTH; round++) {
            long random = LongUtils.randomNonNegative(1, SECURE_RANDOM);
            success = random == 0L;
            if (success) {
                break;
            }
        }
        Assert.assertTrue(success);
        // 测试输出结果范围
        for (int bound = 1; bound < UPPER_BOUND; bound++) {
            for (int round = 0; round < RANDOM_ROUND; round++) {
                long random = LongUtils.randomNonNegative(bound, SECURE_RANDOM);
                Assert.assertTrue(random >= 0 && random < bound);
            }
        }
    }

    @Test
    public void testRandomPositive() {
        try {
            LongUtils.randomPositive(0, SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random positive with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            LongUtils.randomPositive(1, SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random positive with n = 1");
        } catch (AssertionError ignored) {

        }
        try {
            LongUtils.randomPositive(-1, SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random positive with negative n");
        } catch (AssertionError ignored) {

        }
        // 测试输出结果范围
        for (int bound = 2; bound < UPPER_BOUND; bound++) {
            for (int round = 0; round < RANDOM_ROUND; round++) {
                long random = LongUtils.randomPositive(bound, SECURE_RANDOM);
                Assert.assertTrue(random > 0 && random < bound);
            }
        }
    }
}
