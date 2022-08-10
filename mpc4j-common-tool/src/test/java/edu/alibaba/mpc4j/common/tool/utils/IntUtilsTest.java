package edu.alibaba.mpc4j.common.tool.utils;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * 整数工具类测试。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class IntUtilsTest {
    /**
     * 最大迭代次数
     */
    private static final int MAX_ITERATIONS = 100;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testInvalidIntByteArray() {
        try {
            // 尝试转换长度为0的字节数组
            IntUtils.byteArrayToInt(new byte[0]);
            throw new IllegalStateException("ERROR: successfully convert byte array with length 0 to int");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试转换Integer.BYTES - 1长的字节数组
            IntUtils.byteArrayToInt(new byte[Integer.BYTES - 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte array with length Integer.BYTES - 1 to int"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试转换Integer.BYTES + 1的字节数组
            IntUtils.byteArrayToInt(new byte[Integer.BYTES + 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte array with length Integer.BYTES + 1 to int"
            );
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testIntByteArray() {
        testIntByteArray(0, new byte[] { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, });
        testIntByteArray(1, new byte[] { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, });
        testIntByteArray(-1, new byte[] { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, });
        testIntByteArray(Integer.MAX_VALUE, new byte[] { (byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, });
        testIntByteArray(Integer.MIN_VALUE, new byte[] { (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00, });
    }

    private void testIntByteArray(int value, byte[] byteArray) {
        byte[] convertByteArray = IntUtils.intToByteArray(value);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        int convertValue = IntUtils.byteArrayToInt(byteArray);
        Assert.assertEquals(value, convertValue);
    }

    @Test
    public void testInvalidBoundedIntByteArray() {
        try {
            // 尝试转换负数
            IntUtils.boundedIntToByteArray(-1, Byte.MAX_VALUE);
            throw new IllegalStateException("ERROR: successfully convert negative bounded int to byte array");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将比Byte.MAX_VALUE大的整数转换至Byte.MAX_VALUE
            IntUtils.boundedIntToByteArray(Byte.MAX_VALUE + 1, Byte.MAX_VALUE);
            throw new IllegalStateException(
                "ERROR: successfully convert Byte.MAX_VALUE + 1 with bound Byte.MAX_VALUE to byte array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将比Short.MAX_VALUE大的整数转换至Short.MAX_VALUE
            IntUtils.boundedIntToByteArray(Short.MAX_VALUE + 1, Short.MAX_VALUE);
            throw new IllegalStateException(
                "ERROR: successfully convert Short.MAX_VALUE + 1 with bound Short.MAX_VALUE to byte array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将表示负数的字节数组转换为有界int
            IntUtils.byteArrayToBoundedInt(new byte[] { (byte)0xFF }, Byte.MAX_VALUE);
            throw new IllegalStateException(
                "ERROR: successfully convert byte array for negative int to bounded int"
            );
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testBoundedIntByteArray() {
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // Byte.MAX_VALUE附近转换
            int smallByteValue = SECURE_RANDOM.nextInt(Byte.MAX_VALUE / 2 + 1);
            testBoundedIntByteArray(smallByteValue, Byte.MAX_VALUE / 2 + 1);
            int byteValue = SECURE_RANDOM.nextInt(Byte.MAX_VALUE);
            testBoundedIntByteArray(byteValue, Byte.MAX_VALUE);
            int largeByteValue = SECURE_RANDOM.nextInt(Byte.MAX_VALUE * 2 - 1);
            testBoundedIntByteArray(largeByteValue, Byte.MAX_VALUE * 2 - 1);
            // Short.MAX_VALUE附近转换
            int smallShortValue = SECURE_RANDOM.nextInt(Short.MAX_VALUE / 2 + 1);
            testBoundedIntByteArray(smallShortValue, Short.MAX_VALUE / 2 + 1);
            int shortValue = SECURE_RANDOM.nextInt(Short.MAX_VALUE);
            testBoundedIntByteArray(shortValue, Short.MAX_VALUE);
            int largeShortValue = SECURE_RANDOM.nextInt(Short.MAX_VALUE * 2 - 1);
            testBoundedIntByteArray(largeShortValue, Short.MAX_VALUE * 2 - 1);
            // Integer.MAX_VALUE附近转换
            int smallIntValue = SECURE_RANDOM.nextInt(Integer.MAX_VALUE / 2 + 1);
            testBoundedIntByteArray(smallIntValue, Integer.MAX_VALUE / 2 + 1);
            int intValue = SECURE_RANDOM.nextInt(Integer.MAX_VALUE);
            testBoundedIntByteArray(intValue, Integer.MAX_VALUE);
        }
    }

    private void testBoundedIntByteArray(int value, int bound) {
        byte[] convertByteArray = IntUtils.boundedIntToByteArray(value, bound);
        // 验证长度
        if (bound <= Byte.MAX_VALUE) {
            Assert.assertEquals(convertByteArray.length, Byte.BYTES);
        } else if (bound <= Short.MAX_VALUE) {
            Assert.assertEquals(convertByteArray.length, Short.BYTES);
        } else {
            Assert.assertEquals(convertByteArray.length, Integer.BYTES);
        }
        int convertValue = IntUtils.byteArrayToBoundedInt(convertByteArray, bound);
        Assert.assertEquals(value, convertValue);

    }

    @Test
    public void testInvalidIntFixedByteArray() {
        try {
            // 尝试将int转换成长度为0的字节数组
            IntUtils.nonNegIntToFixedByteArray(0, 0);
            throw new IllegalStateException("ERROR: successfully convert int to byte array with length 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将负数转换成固定长度字节数组
            IntUtils.nonNegIntToFixedByteArray(-1, Integer.BYTES);
            throw new IllegalStateException("ERROR: successfully convert negative int to byte array");
        } catch (AssertionError ignored) {

        }
        try {
            // 验证截断转换越界处理
            IntUtils.nonNegIntToFixedByteArray(256, 1);
            throw new IllegalStateException("ERROR: successfully convert 128 to byte array with byte length 1");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为0的字节数组转换为int
            IntUtils.fixedByteArrayToNonNegInt(new byte[0]);
            throw new IllegalStateException("ERROR: successfully convert byte array with length 0 to int");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将代表负数的字节数组转换为int
            IntUtils.fixedByteArrayToNonNegInt(new byte[] { (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00});
            throw new IllegalStateException("ERROR: successfully convert byte array to negative int");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testIntFixedByteArray() {
        // 0的转换
        testIntFixedByteArray(0, new byte[] { 0x00, 0x00, 0x00, 0x00, });
        testIntFixedByteArray(0, new byte[] { 0x00, });
        testIntFixedByteArray(0, new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, });
        // 正数转换
        testIntFixedByteArray(1, new byte[] { 0x00, 0x00, 0x00, 0x01, });
        testIntFixedByteArray(1, new byte[] { 0x01, });
        testIntFixedByteArray(1, new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, });
        // 最大值转换
        testIntFixedByteArray((1 << Byte.SIZE) - 1, new byte[] { (byte) 0xFF, });
        testIntFixedByteArray((1 << 2 * Byte.SIZE) - 1, new byte[] { (byte) 0xFF, (byte) 0xFF, });
        testIntFixedByteArray((1 << 3 * Byte.SIZE) - 1, new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, });
        testIntFixedByteArray(Integer.MAX_VALUE, new byte[] { (byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, });
        testIntFixedByteArray(Integer.MAX_VALUE, new byte[] { 0x00, (byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, });
    }

    private void testIntFixedByteArray(int value, byte[] byteArray) {
        byte[] convertByteArray = IntUtils.nonNegIntToFixedByteArray(value, byteArray.length);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        int convertValue = IntUtils.fixedByteArrayToNonNegInt(byteArray);
        Assert.assertEquals(value, convertValue);
    }

    @Test
    public void testInvalidIntArrayByteArray() {
        try {
            // 尝试将长度为0的int数组转换成字节数组
            IntUtils.intArrayToByteArray(new int[0]);
            throw new IllegalStateException("ERROR: successfully convert int[] with length 0 to byte array");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为0的字节数组转换成int数组
            IntUtils.byteArrayToIntArray(new byte[0]);
            throw new IllegalStateException("ERROR: successfully convert byte[] with length 0 to int array");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为Integer.BYTES - 1的字节数组转换成int数组
            IntUtils.byteArrayToIntArray(new byte[Integer.BYTES - 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length Integer.BYTES - 1 to int array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为Integer.BYTES + 1的字节数组转换成int数组
            IntUtils.byteArrayToIntArray(new byte[Integer.BYTES + 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length Integer.BYTES + 1 to int array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为2 * Integer.BYTES - 1的字节数组转换成int数组
            IntUtils.byteArrayToIntArray(new byte[2 * Integer.BYTES - 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length 2 * Integer.BYTES - 1 to int array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为2 * Integer.BYTES + 1的字节数组转换成int数组
            IntUtils.byteArrayToIntArray(new byte[2 * Integer.BYTES + 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length 2 * Integer.BYTES + 1 to int array"
            );
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testIntArrayByteArray() {
        testIntArrayByteArray(new int[] { 0x00 }, new byte[] {0x00, 0x00, 0x00, 0x00, });
        testIntArrayByteArray(
            new int[] { Integer.MIN_VALUE, 0, -1, 1, Integer.MAX_VALUE },
            new byte[] {
                (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01,
                (byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            }
        );
    }

    private void testIntArrayByteArray(int[] intArray, byte[] byteArray) {
        byte[] convertByteArray = IntUtils.intArrayToByteArray(intArray);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        int[] convertIntArray = IntUtils.byteArrayToIntArray(byteArray);
        Assert.assertArrayEquals(intArray, convertIntArray);
    }
}
