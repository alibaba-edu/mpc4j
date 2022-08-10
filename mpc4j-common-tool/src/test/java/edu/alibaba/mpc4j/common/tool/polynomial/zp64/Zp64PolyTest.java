package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory.Zp64PolyType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Zp64多项式插值测试。
 *
 * @author Weiran Liu
 * @date 2022/01/05
 */
@RunWith(Parameterized.class)
public class Zp64PolyTest {
    /**
     * 默认l
     */
    private static final int DEFAULT_L = 40;
    /**
     * 测试l
     */
    private static final int[] L_ARRAY = new int[] {20, 39, 40, 41, 61, 62};
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
        configurationParams.add(new Object[] {Zp64PolyType.NTL.name(), Zp64PolyType.NTL,});
        // RINGS_NEWTON
        configurationParams.add(new Object[] {Zp64PolyType.RINGS_NEWTON.name(), Zp64PolyType.RINGS_NEWTON,});
        // RINGS_LAGRANGE
        configurationParams.add(new Object[] {Zp64PolyType.RINGS_LAGRANGE.name(), Zp64PolyType.RINGS_LAGRANGE,});

        return configurationParams;
    }

    private final Zp64PolyType type;

    public Zp64PolyTest(String name, Zp64PolyType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, DEFAULT_L);
        Assert.assertEquals(type, zp64Poly.getType());
    }

    @Test
    public void testIllegalInputs() {
        // 尝试设置l = 0
        try {
            Zp64PolyFactory.createInstance(type, 0);
            throw new IllegalStateException("ERROR: successfully create ZpPoly with l = 0");
        } catch (AssertionError ignored) {

        }
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, DEFAULT_L);
        // 尝试对给定的元素数量少于实际元素数量插值
        try {
            long[] xArray = LongStream.range(0, DEFAULT_NUM).toArray();
            long[] yArray = LongStream.range(0, DEFAULT_NUM).toArray();
            zp64Poly.interpolate(DEFAULT_NUM / 2, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate with DEFAULT_NUM < actual pairs");
        } catch (AssertionError ignored) {

        }
        // 尝试对不合法的输入点插值
        try {
            long[] xArray = LongStream.range(-DEFAULT_NUM, 0).toArray();
            long[] yArray = LongStream.range(0, DEFAULT_NUM).toArray();
            zp64Poly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate large values");
        } catch (AssertionError ignored) {

        }
        // 尝试对不相等的数据虚拟插值
        try {
            long[] xArray = LongStream.range(0, DEFAULT_NUM).toArray();
            long[] yArray = LongStream.range(0, DEFAULT_NUM / 2).toArray();
            zp64Poly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate points with unequal size");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testEmptyInterpolation() {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, DEFAULT_L);
        long[] xArray = new long[0];
        long[] yArray = new long[0];
        // 没有插值点，但要补充随机点
        long[] coefficients = zp64Poly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficient(zp64Poly, DEFAULT_NUM, coefficients);
    }

    @Test
    public void testOneInterpolation() {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, DEFAULT_L);
        long p = zp64Poly.getPrime();
        long[] xArray = IntStream.range(0, 1)
            .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray();
        long[] yArray = IntStream.range(0, 1)
            .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray();
        // 只有1组插值点
        long[] coefficients = zp64Poly.interpolate(1, xArray, yArray);
        assertCoefficient(zp64Poly, 1, coefficients);
        assertEvaluate(zp64Poly, coefficients, xArray, yArray);
        // 只有1组插值点，但要补充随机点
        coefficients = zp64Poly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficient(zp64Poly, DEFAULT_NUM, coefficients);
        assertEvaluate(zp64Poly, coefficients, xArray, yArray);
    }

    @Test
    public void testConstantInterpolation() {
        for (int l : L_ARRAY) {
            testConstantInterpolation(l);
        }
    }

    private void testConstantInterpolation(int l) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, l);
        long[] xArray = LongStream.range(0, DEFAULT_NUM / 2).toArray();
        long[] yArray =LongStream.range(0, DEFAULT_NUM / 2).toArray();
        long[] coefficients = zp64Poly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficient(zp64Poly, DEFAULT_NUM, coefficients);
        assertEvaluate(zp64Poly, coefficients, xArray, yArray);
        // 多项式仍然过(0,0)点，因此常数项仍然为0，但其他位应该均不为0
        Assert.assertEquals(0L, coefficients[0]);
        IntStream.range(1, coefficients.length).forEach(i -> Assert.assertNotEquals(0L, coefficients[i]));
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
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, l);
        long p = zp64Poly.getPrime();
        long[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray();
        long[] yArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray();
        // 插值一半的点
        long[] coefficients = zp64Poly.interpolate(DEFAULT_NUM / 2, xArray, yArray);
        assertCoefficient(zp64Poly, DEFAULT_NUM / 2, coefficients);
        assertEvaluate(zp64Poly, coefficients, xArray, yArray);
        // 插值一半的点，补充随机点
        coefficients = zp64Poly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficient(zp64Poly, DEFAULT_NUM, coefficients);
        assertEvaluate(zp64Poly, coefficients, xArray, yArray);
    }

    @Test
    public void testParallel() {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, DEFAULT_L);
        long p = zp64Poly.getPrime();
        ArrayList<long[]> xArrayList = new ArrayList<>();
        ArrayList<long[]> yArrayList = new ArrayList<>();
        IntStream.range(0, MAX_PARALLEL).forEach(parallelIndex -> {
            long[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray();
            long[] yArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray();
            xArrayList.add(xArray);
            yArrayList.add(yArray);
        });
        IntStream.range(0, MAX_PARALLEL).parallel().forEach(parallelIndex -> {
            long[] xArray = xArrayList.get(parallelIndex);
            long[] yArray = yArrayList.get(parallelIndex);
            long[] coefficients = zp64Poly.interpolate(DEFAULT_NUM, xArray, yArray);
            assertEvaluate(zp64Poly, coefficients, xArray, yArray);
        });
    }

    private void assertCoefficient(Zp64Poly zp64Poly, int num, long[] coefficients) {
        long p = zp64Poly.getPrime();
        Assert.assertEquals(zp64Poly.coefficientNum(num), coefficients.length);
        Arrays.stream(coefficients).forEach(coefficient -> {
            Assert.assertTrue(coefficient >= 0);
            Assert.assertTrue(coefficient < p);
        });
    }

    private void assertEvaluate(Zp64Poly zp64Poly, long[] coefficients, long[] xArray, long[] yArray) {
        // 逐一求值
        IntStream.range(0, xArray.length).forEach(index -> {
            long evaluation = zp64Poly.evaluate(coefficients, xArray[index]);
            Assert.assertEquals(yArray[index], evaluation);
        });
        // 批量求值
        long[] evaluations = zp64Poly.evaluate(coefficients, xArray);
        Assert.assertArrayEquals(yArray, evaluations);
    }

    @Test
    public void testEmptyRootInterpolation() {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, DEFAULT_L);
        long p = zp64Poly.getPrime();
        long[] xArray = new long[0];
        long y = LongUtils.randomNonNegative(p, SECURE_RANDOM);
        // 没有插值点，但要补充随机点
        long[] coefficients = zp64Poly.rootInterpolate(DEFAULT_NUM, xArray, y);
        assertRootCoefficient(zp64Poly, DEFAULT_NUM, coefficients);
    }

    @Test
    public void testOneRootInterpolation() {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, DEFAULT_L);
        long p = zp64Poly.getPrime();
        long[] xArray = IntStream.range(0, 1)
            .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray();
        long y = LongUtils.randomNonNegative(p, SECURE_RANDOM);
        // 只有1组插值点
        long[] coefficients = zp64Poly.rootInterpolate(1, xArray, y);
        assertRootCoefficient(zp64Poly, 1, coefficients);
        assertRootEvaluate(zp64Poly, coefficients, xArray, y);
        // 只有1组插值点，但要补充随机点
        coefficients = zp64Poly.rootInterpolate(DEFAULT_NUM, xArray, y);
        assertRootCoefficient(zp64Poly, DEFAULT_NUM, coefficients);
        assertRootEvaluate(zp64Poly, coefficients, xArray, y);
    }

    @Test
    public void testRandomRootInterpolation() {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            for (int l : L_ARRAY) {
                testRandomRootInterpolation(l);
            }
        }
    }

    private void testRandomRootInterpolation(int l) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, l);
        long p = zp64Poly.getPrime();
        long[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray();
        long y = LongUtils.randomNonNegative(p, SECURE_RANDOM);
        // 插值一半的点
        long[] coefficients = zp64Poly.rootInterpolate(DEFAULT_NUM / 2, xArray, y);
        assertRootCoefficient(zp64Poly, DEFAULT_NUM / 2, coefficients);
        assertRootEvaluate(zp64Poly, coefficients, xArray, y);
        // 插值一半的点，补充随机点
        coefficients = zp64Poly.rootInterpolate(DEFAULT_NUM, xArray, y);
        assertRootCoefficient(zp64Poly, DEFAULT_NUM, coefficients);
        assertRootEvaluate(zp64Poly, coefficients, xArray, y);
    }

    @Test
    public void testRootParallel() {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, DEFAULT_L);
        long p = zp64Poly.getPrime();
        ArrayList<long[]> xArrayList = new ArrayList<>();
        long[] yArray = new long[MAX_PARALLEL];
        IntStream.range(0, MAX_PARALLEL).forEach(parallelIndex -> {
            long[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray();
            xArrayList.add(xArray);
            yArray[parallelIndex] = LongUtils.randomNonNegative(p, SECURE_RANDOM);
        });
        IntStream.range(0, MAX_PARALLEL).parallel().forEach(parallelIndex -> {
            long[] xArray = xArrayList.get(parallelIndex);
            long y = yArray[parallelIndex];
            long[] coefficients = zp64Poly.rootInterpolate(DEFAULT_NUM, xArray, y);
            assertRootEvaluate(zp64Poly, coefficients, xArray, y);
        });
    }

    private void assertRootCoefficient(Zp64Poly zp64Poly, int num, long[] coefficients) {
        long p = zp64Poly.getPrime();
        Assert.assertEquals(zp64Poly.rootCoefficientNum(num), coefficients.length);
        Arrays.stream(coefficients).forEach(coefficient -> {
            Assert.assertTrue(coefficient >= 0);
            Assert.assertTrue(coefficient < p);
        });
    }

    private void assertRootEvaluate(Zp64Poly zp64Poly, long[] coefficients, long[] xArray, long y) {
        // 逐一求值
        Arrays.stream(xArray)
            .map(x -> zp64Poly.evaluate(coefficients, x))
            .forEach(evaluation -> Assert.assertEquals(y, evaluation));
        // 批量求值
        Arrays.stream(zp64Poly.evaluate(coefficients, xArray))
            .forEach(evaluation -> Assert.assertEquals(y, evaluation));
    }
}
