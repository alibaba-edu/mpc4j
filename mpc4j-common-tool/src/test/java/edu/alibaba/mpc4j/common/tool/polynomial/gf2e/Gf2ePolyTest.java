package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory.Gf2ePolyType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * GF2E多项式插值测试。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
@RunWith(Parameterized.class)
public class Gf2ePolyTest {
    /**
     * 默认l
     */
    private static final int DEFAULT_L = 40;
    /**
     * 测试l
     */
    private static final int[] L_ARRAY = new int[] {39, 40, 41, 127, 128, 129};
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 5;
    /**
     * 插值点数量
     */
    private static final int DEFAULT_NUM = 20;
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 10;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // NTL
        configurationParams.add(new Object[] {Gf2ePolyType.NTL.name(), Gf2ePolyType.NTL,});
        // RINGS_NEWTON
        configurationParams.add(new Object[] {Gf2ePolyType.RINGS_NEWTON.name(), Gf2ePolyType.RINGS_NEWTON,});
        // RINGS_LAGRANGE
        configurationParams.add(new Object[] {Gf2ePolyType.RINGS_LAGRANGE.name(), Gf2ePolyType.RINGS_LAGRANGE,});

        return configurationParams;
    }

    private final Gf2ePolyType type;

    public Gf2ePolyTest(String name, Gf2ePolyType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, DEFAULT_L);
        Assert.assertEquals(type, gf2ePoly.getType());
    }

    @Test
    public void testIllegalInputs() {
        // 尝试设置l = 0
        try {
            Gf2ePolyFactory.createInstance(type, 0);
            throw new IllegalStateException("ERROR: successfully create Gf2xPoly with l = 0");
        } catch (AssertionError ignored) {

        }
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, DEFAULT_L - 1);
        int l = gf2ePoly.getL();
        int byteL = gf2ePoly.getByteL();
        // 尝试对给定的元素数量少于实际元素数量插值
        try {
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            gf2ePoly.interpolate(DEFAULT_NUM / 2, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate with DEFAULT_NUM < actual pairs");
        } catch (AssertionError ignored) {

        }
        // 尝试对大于l比特长度的插值对插值
        try {
            int largeL = l + 1;
            int largeByteL = CommonUtils.getByteLength(largeL);
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BytesUtils.randomByteArray(largeByteL, largeL, SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BytesUtils.randomByteArray(largeByteL, largeL, SECURE_RANDOM))
                .toArray(byte[][]::new);
            gf2ePoly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate large values");
        } catch (AssertionError ignored) {

        }
        // 尝试对不相等的数据插值
        try {
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] yArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            gf2ePoly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate points with unequal size");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testEmptyInterpolation() {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, DEFAULT_L);
        byte[][] xArray = new byte[0][];
        byte[][] yArray = new byte[0][];
        // 没有插值点，但要补充随机点
        byte[][] coefficients = gf2ePoly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficients(gf2ePoly, DEFAULT_NUM, coefficients);
    }

    @Test
    public void testOneInterpolation() {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, DEFAULT_L);
        int l = gf2ePoly.getL();
        int byteL = gf2ePoly.getByteL();
        byte[][] xArray = IntStream.range(0, 1)
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
        byte[][] yArray = IntStream.range(0, 1)
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
        // 只有1组插值点
        byte[][] coefficients = gf2ePoly.interpolate(1, xArray, yArray);
        assertCoefficients(gf2ePoly, 1, coefficients);
        assertEvaluate(gf2ePoly, coefficients, xArray, yArray);
        // 只有1组插值点，但要补充随机点
        coefficients = gf2ePoly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficients(gf2ePoly, DEFAULT_NUM, coefficients);
        assertEvaluate(gf2ePoly, coefficients, xArray, yArray);
    }

    @Test
    public void testConstantInterpolation() {
        for (int l : L_ARRAY) {
            testConstantInterpolation(l);
        }
    }

    private void testConstantInterpolation(int l) {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, l);
        int byteL = gf2ePoly.getByteL();
        byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(BigInteger::valueOf)
            .map(x -> BigIntegerUtils.nonNegBigIntegerToByteArray(x, byteL))
            .toArray(byte[][]::new);
        byte[][] yArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(BigInteger::valueOf)
            .map(x -> BigIntegerUtils.nonNegBigIntegerToByteArray(x, byteL))
            .toArray(byte[][]::new);
        byte[][] coefficients = gf2ePoly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficients(gf2ePoly, DEFAULT_NUM, coefficients);
        assertEvaluate(gf2ePoly, coefficients, xArray, yArray);
        byte[] zero = new byte[byteL];
        // 多项式仍然过(0,0)点，因此常数项仍然为0，但其他位应该均不为0
        Assert.assertArrayEquals(zero, coefficients[0]);
        IntStream.range(1, coefficients.length).forEach(i ->
            Assert.assertNotEquals(ByteBuffer.wrap(zero), ByteBuffer.wrap(coefficients[i]))
        );
    }

    @Test
    public void testRandomInterpolation() {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            for (int l : L_ARRAY) {
                testRandomInterpolation(l);
            }
        }
    }

    private void testRandomInterpolation(int l) {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, l);
        int byteL = gf2ePoly.getByteL();
        byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
        byte[][] yArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
        // 插值一半的点
        byte[][] coefficients = gf2ePoly.interpolate(DEFAULT_NUM / 2, xArray, yArray);
        assertCoefficients(gf2ePoly, DEFAULT_NUM / 2, coefficients);
        assertEvaluate(gf2ePoly, coefficients, xArray, yArray);
        // 插值一半的点，补充随机点
        coefficients = gf2ePoly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficients(gf2ePoly, DEFAULT_NUM, coefficients);
        assertEvaluate(gf2ePoly, coefficients, xArray, yArray);
    }

    @Test
    public void testParallel() {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, DEFAULT_L);
        int l = gf2ePoly.getL();
        int byteL = gf2ePoly.getByteL();
        ArrayList<byte[][]> xArrayList = new ArrayList<>();
        ArrayList<byte[][]> yArrayList = new ArrayList<>();
        IntStream.range(0, MAX_PARALLEL).forEach(parallelIndex -> {
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] yArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            xArrayList.add(xArray);
            yArrayList.add(yArray);
        });
        IntStream.range(0, MAX_PARALLEL).parallel().forEach(parallelIndex -> {
            byte[][] xArray = xArrayList.get(parallelIndex);
            byte[][] yArray = yArrayList.get(parallelIndex);
            byte[][] coefficients = gf2ePoly.interpolate(DEFAULT_NUM, xArray, yArray);
            assertEvaluate(gf2ePoly, coefficients, xArray, yArray);
        });
    }

    private void assertCoefficients(Gf2ePoly gf2ePoly, int num, byte[][] coefficients) {
        Assert.assertEquals(gf2ePoly.coefficientNum(num), coefficients.length);
        int l = gf2ePoly.getL();
        int byteL = gf2ePoly.getByteL();
        Arrays.stream(coefficients).forEach(coefficient -> {
            Assert.assertEquals(byteL, coefficient.length);
            Assert.assertTrue(BytesUtils.isReduceByteArray(coefficient, l));
        });
    }

    private void assertEvaluate(Gf2ePoly gf2ePoly, byte[][] coefficients, byte[][] xArray, byte[][] yArray) {
        // 逐一求值
        IntStream.range(0, xArray.length).forEach(index -> {
            byte[] evaluation = gf2ePoly.evaluate(coefficients, xArray[index]);
            Assert.assertArrayEquals(yArray[index], evaluation);
        });
        // 批量求值
        byte[][] evaluations = gf2ePoly.evaluate(coefficients, xArray);
        Assert.assertArrayEquals(yArray, evaluations);
    }

    @Test
    public void testEmptyRootInterpolation() {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, DEFAULT_L);
        int l = gf2ePoly.getL();
        int byteL = gf2ePoly.getByteL();
        byte[][] xArray = new byte[0][];
        byte[] y = BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM);
        // 没有插值点，但要补充随机点
        byte[][] coefficients = gf2ePoly.rootInterpolate(DEFAULT_NUM, xArray, y);
        assertRootCoefficients(gf2ePoly, DEFAULT_NUM, coefficients);
    }

    @Test
    public void testOneRootInterpolation() {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, DEFAULT_L);
        int l = gf2ePoly.getL();
        int byteL = gf2ePoly.getByteL();
        // 只存在一组插值点，也应该可以构建多项式
        byte[][] xArray = IntStream.range(0, 1)
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
        byte[] y = BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM);
        // 只有1组插值点
        byte[][] coefficients = gf2ePoly.rootInterpolate(1, xArray, y);
        assertRootCoefficients(gf2ePoly, 1, coefficients);
        assertRootEvaluate(gf2ePoly, coefficients, xArray, y);
        // 只有1组插值点，但要补充随机点
        coefficients = gf2ePoly.rootInterpolate(DEFAULT_NUM, xArray, y);
        assertRootCoefficients(gf2ePoly, DEFAULT_NUM, coefficients);
        assertRootEvaluate(gf2ePoly, coefficients, xArray, y);
    }

    @Test
    public void testRandomRootInterpolation() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            for (int l : L_ARRAY) {
                testRandomRootInterpolation(l);
            }
        }
    }

    private void testRandomRootInterpolation(int l) {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, l);
        int byteL = gf2ePoly.getByteL();
        byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
        byte[] y = BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM);
        // 插值一半的点
        byte[][] coefficients = gf2ePoly.rootInterpolate(DEFAULT_NUM / 2, xArray, y);
        assertRootCoefficients(gf2ePoly, DEFAULT_NUM / 2, coefficients);
        assertRootEvaluate(gf2ePoly, coefficients, xArray, y);
        // 插值一半的点，补充随机点
        coefficients = gf2ePoly.rootInterpolate(DEFAULT_NUM, xArray, y);
        assertRootCoefficients(gf2ePoly, DEFAULT_NUM, coefficients);
        assertRootEvaluate(gf2ePoly, coefficients, xArray, y);
    }

    @Test
    public void testRootParallel() {
        Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, DEFAULT_L);
        int l = gf2ePoly.getL();
        int byteL = gf2ePoly.getByteL();
        ArrayList<byte[][]> xArrayList = new ArrayList<>();
        ArrayList<byte[]> yList = new ArrayList<>();
        IntStream.range(0, MAX_PARALLEL).forEach(parallelIndex -> {
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[] y = BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM);
            xArrayList.add(xArray);
            yList.add(y);
        });
        IntStream.range(0, MAX_PARALLEL).forEach(parallelIndex -> {
            byte[][] xArray = xArrayList.get(parallelIndex);
            byte[] y = yList.get(parallelIndex);
            byte[][] coefficients = gf2ePoly.rootInterpolate(DEFAULT_NUM, xArray, y);
            assertRootEvaluate(gf2ePoly, coefficients, xArray, y);
        });
    }

    private void assertRootCoefficients(Gf2ePoly gf2ePoly, int num, byte[][] coefficients) {
        Assert.assertEquals(gf2ePoly.rootCoefficientNum(num), coefficients.length);
        int l = gf2ePoly.getL();
        int byteL = gf2ePoly.getByteL();
        Arrays.stream(coefficients).forEach(coefficient -> {
            Assert.assertEquals(byteL, coefficient.length);
            Assert.assertTrue(BytesUtils.isReduceByteArray(coefficient, l));
        });
    }

    private void assertRootEvaluate(Gf2ePoly gf2ePoly, byte[][] coefficients, byte[][] xArray, byte[] y) {
        // 逐一求值
        Arrays.stream(xArray)
            .map(x -> gf2ePoly.evaluate(coefficients, x))
            .forEach(evaluation -> Assert.assertArrayEquals(y, evaluation));
        // 批量求值
        Arrays.stream(gf2ePoly.evaluate(coefficients, xArray))
            .forEach(evaluation -> Assert.assertArrayEquals(y, evaluation));
    }
}
