package edu.alibaba.mpc4j.common.tool.utils;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 浮点数工具类测试。
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class DoubleUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoubleUtilsTest.class);
    /**
     * 最大迭代次数
     */
    private static final int MAX_ITERATIONS = 100;
    /**
     * 最大数组长度
     */
    private static final int MAX_ARRAY_LENGTH = 10;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testInvalidDoubleByteArray() {
        try {
            // 尝试转换长度为0的字节数组
            DoubleUtils.byteArrayToDouble(new byte[0]);
            throw new IllegalStateException("ERROR: successfully convert byte array with length 0 to double");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试转换过短的字节数组
            DoubleUtils.byteArrayToDouble(new byte[Double.BYTES - 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte array with length Double.BYTES - 1 to double"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试转换过长的字节数组
            DoubleUtils.byteArrayToDouble(new byte[Double.BYTES + 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte array with length Double.BYTES + 1 to double"
            );
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testDoubleByteArray() {
        // double的表示方法比较复杂，这里采用随机转换
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double value = SECURE_RANDOM.nextDouble() * SECURE_RANDOM.nextInt();
            byte[] byteArray = DoubleUtils.doubleToByteArray(value);
            double convertValue = DoubleUtils.byteArrayToDouble(byteArray);
            Assert.assertEquals(value, convertValue, DoubleUtils.PRECISION);
        }
    }

    @Test
    public void testInvalidDoubleArrayByteArray() {
        try {
            // 尝试将长度为0的double数组转换成字节数组
            DoubleUtils.doubleArrayToByteArray(new double[0]);
            throw new IllegalStateException("ERROR: successfully convert double[] with length 0 to byte array");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为0的字节数组转换成int数组
            DoubleUtils.byteArrayToDoubleArray(new byte[0]);
            throw new IllegalStateException("ERROR: successfully convert byte[] with length 0 to double array");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为Double.BYTES - 1的字节数组转换成double数组
            DoubleUtils.byteArrayToDoubleArray(new byte[Double.BYTES - 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length Double.BYTES - 1 to double array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为Double.BYTES + 1的字节数组转换成int数组
            DoubleUtils.byteArrayToDoubleArray(new byte[Double.BYTES + 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length Double.BYTES + 1 to double array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为2 * Double.BYTES - 1的字节数组转换成int数组
            DoubleUtils.byteArrayToDoubleArray(new byte[2 * Double.BYTES - 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length 2 * Double.BYTES - 1 to double array"
            );
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试将长度为2 * Double.BYTES + 1的字节数组转换成int数组
            DoubleUtils.byteArrayToDoubleArray(new byte[2 * Double.BYTES + 1]);
            throw new IllegalStateException(
                "ERROR: successfully convert byte[] with length 2 * Double.BYTES + 1 to double array"
            );
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testDoubleArrayByteArray() {
        // double的表示方法比较复杂，这里采用随机转换
        for (int arrayLength = 1; arrayLength < MAX_ARRAY_LENGTH; arrayLength++) {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                double[] values = IntStream.range(0, MAX_ARRAY_LENGTH)
                    .mapToDouble(index -> SECURE_RANDOM.nextDouble() * SECURE_RANDOM.nextInt())
                    .toArray();
                byte[] byteArray = DoubleUtils.doubleArrayToByteArray(values);
                double[] convertValues = DoubleUtils.byteArrayToDoubleArray(byteArray);
                Assert.assertArrayEquals(values, convertValues, DoubleUtils.PRECISION);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testInvalidEstimateCombinatorial() {
        try {
            DoubleUtils.estimateCombinatorial(-1, 0);
            throw new IllegalStateException("ERROR: successfully compute C(-1, 0)");
        } catch (AssertionError ignored) {

        }

        try {
            DoubleUtils.estimateCombinatorial(10, -1);
            throw new IllegalStateException("ERROR: successfully compute C(10, -1)");
        } catch (AssertionError ignored) {

        }

        try {
            DoubleUtils.estimateCombinatorial(10, 11);
            throw new IllegalStateException("ERROR: successfully compute C(10, 11)");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testEstimateCombinatorial() {
        // C(1, 0) = 1
        testEstimateCombinatorial(1, 0, 1L);
        // C(1, 1) = 1
        testEstimateCombinatorial(1, 1, 1L);
        // C(10, 0) = 1
        testEstimateCombinatorial(10, 0, 1L);
        // C(10, 1) = 10
        testEstimateCombinatorial(10, 1, 10L);
        // C(10, 9) = 10
        testEstimateCombinatorial(10, 9, 10L);
        // C(10, 10) = 1
        testEstimateCombinatorial(10, 10, 1L);
        // C(10, 5) = 252
        testEstimateCombinatorial(10, 5, 252L);
        // C(10, 3) = 120
        testEstimateCombinatorial(10, 3, 120L);
        // C(10, 6) = 210
        testEstimateCombinatorial(10, 6, 210L);
    }

    private void testEstimateCombinatorial(int n, int m, long truth) {
        double combinatorial = DoubleUtils.estimateCombinatorial(n, m);
        Assert.assertEquals(truth, combinatorial, DoubleUtils.PRECISION);
    }

    @Test
    public void testCombinatorialEfficiency() {
        LOGGER.info("-----测试组合数计算效率-----");
        int n = 1024;
        int m = 128;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        BigIntegerUtils.binomial(n, m);
        stopWatch.stop();
        LOGGER.info("BigInteger计算C({}, {})时间 = {}us", n, m, stopWatch.getTime(TimeUnit.MICROSECONDS));
        stopWatch.reset();

        stopWatch.start();
        //noinspection ResultOfMethodCallIgnored
        DoubleUtils.estimateCombinatorial(n, m);
        stopWatch.stop();
        LOGGER.info("double计算C({}, {})时间 = {}us", n, m, stopWatch.getTime(TimeUnit.MICROSECONDS));
        stopWatch.reset();
    }
}
