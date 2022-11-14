package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.polynomial.zp.ZpPolyFactory.ZpPolyType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Zp多项式插值测试。
 *
 * @author Weiran Liu
 * @date 2022/01/05
 */
@RunWith(Parameterized.class)
public class ZpPolyTest {
    /**
     * 默认l
     */
    private static final int DEFAULT_L = 40;
    /**
     * 测试l
     */
    private static final int[] L_ARRAY = new int[]{39, 40, 41, 127, 128, 129};
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
        Collection<Object[]> configurations = new ArrayList<>();
        // NTL
        configurations.add(new Object[]{ZpPolyType.NTL.name(), ZpPolyType.NTL,});
        // RINGS_NEWTON
        configurations.add(new Object[]{ZpPolyType.RINGS_NEWTON.name(), ZpPolyType.RINGS_NEWTON,});
        // JDK_NEWTON
        configurations.add(new Object[]{ZpPolyType.JDK_NEWTON.name(), ZpPolyType.JDK_NEWTON,});
        // RINGS_LAGRANGE
        configurations.add(new Object[]{ZpPolyType.RINGS_LAGRANGE.name(), ZpPolyType.RINGS_LAGRANGE,});
        // JDK_LAGRANGE
        configurations.add(new Object[]{ZpPolyType.JDK_LAGRANGE.name(), ZpPolyType.JDK_LAGRANGE,});

        return configurations;
    }

    private final ZpPolyType type;

    public ZpPolyTest(String name, ZpPolyType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, DEFAULT_L);
        Assert.assertEquals(type, zpPoly.getType());
    }

    @Test
    public void testIllegalInputs() {
        // 尝试设置l = 0
        try {
            ZpPolyFactory.createInstance(type, 0);
            throw new IllegalStateException("ERROR: successfully create ZpPoly with l = 0");
        } catch (AssertionError ignored) {

        }
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, DEFAULT_L);
        BigInteger p = zpPoly.getPrime();
        // 尝试对给定的元素数量少于实际元素数量插值
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            zpPoly.interpolate(DEFAULT_NUM / 2, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate with DEFAULT_NUM < actual pairs");
        } catch (AssertionError ignored) {

        }
        // 尝试对大于p的插值对插值
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM).add(p))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM).add(p))
                .toArray(BigInteger[]::new);
            zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate large values");
        } catch (AssertionError ignored) {

        }
        // 尝试对不相等的数据虚拟插值
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate points with unequal size");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testEmptyInterpolation() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, DEFAULT_L);
        int pointNum = 0;
        BigInteger[] xArray = new BigInteger[pointNum];
        BigInteger[] yArray = new BigInteger[pointNum];
        // 没有插值点，但要补充随机点
        BigInteger[] coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficient(zpPoly, pointNum, DEFAULT_NUM, coefficients);
    }

    @Test
    public void testOneInterpolation() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, DEFAULT_L);
        BigInteger p = zpPoly.getPrime();
        int pointNum = 1;
        BigInteger[] xArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        // 只有1组插值点
        BigInteger[] coefficients = zpPoly.interpolate(pointNum, xArray, yArray);
        assertCoefficient(zpPoly, pointNum, pointNum, coefficients);
        assertEvaluate(zpPoly, coefficients, xArray, yArray);
        // 只有1组插值点，但要补充随机点
        coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficient(zpPoly, pointNum, DEFAULT_NUM, coefficients);
        assertEvaluate(zpPoly, coefficients, xArray, yArray);
    }

    @Test
    public void testConstantInterpolation() {
        for (int l : L_ARRAY) {
            testConstantInterpolation(l);
        }
    }

    private void testConstantInterpolation(int l) {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, l);
        int pointNum = DEFAULT_NUM / 2;
        BigInteger[] xArray = IntStream.range(0, pointNum)
            .mapToObj(BigInteger::valueOf)
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, pointNum)
            .mapToObj(BigInteger::valueOf)
            .toArray(BigInteger[]::new);
        BigInteger[] coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficient(zpPoly, pointNum, DEFAULT_NUM, coefficients);
        assertEvaluate(zpPoly, coefficients, xArray, yArray);
        // 多项式仍然过(0,0)点，因此常数项仍然为0
        Assert.assertEquals(BigInteger.ZERO, coefficients[0]);
        // 但其他位应该均不为0
        IntStream.range(1, coefficients.length).forEach(i -> Assert.assertNotEquals(BigInteger.ZERO, coefficients[i]));
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
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, l);
        BigInteger p = zpPoly.getPrime();
        int pointNum = DEFAULT_NUM / 2;
        BigInteger[] xArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        // 插值一半的点
        BigInteger[] coefficients = zpPoly.interpolate(pointNum, xArray, yArray);
        assertCoefficient(zpPoly, pointNum, pointNum, coefficients);
        assertEvaluate(zpPoly, coefficients, xArray, yArray);
        // 插值一半的点，补充随机点
        coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        assertCoefficient(zpPoly, pointNum, DEFAULT_NUM, coefficients);
        assertEvaluate(zpPoly, coefficients, xArray, yArray);
    }

    @Test
    public void testParallel() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, DEFAULT_L);
        BigInteger p = zpPoly.getPrime();
        int pointNum = DEFAULT_NUM / 2;
        ArrayList<BigInteger[]> xArrayList = new ArrayList<>();
        ArrayList<BigInteger[]> yArrayList = new ArrayList<>();
        IntStream.range(0, MAX_PARALLEL).forEach(parallelIndex -> {
            BigInteger[] xArray = IntStream.range(0, pointNum)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, pointNum)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            xArrayList.add(xArray);
            yArrayList.add(yArray);
        });
        IntStream.range(0, MAX_PARALLEL).parallel().forEach(parallelIndex -> {
            BigInteger[] xArray = xArrayList.get(parallelIndex);
            BigInteger[] yArray = yArrayList.get(parallelIndex);
            BigInteger[] coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
            assertEvaluate(zpPoly, coefficients, xArray, yArray);
        });
    }

    private void assertCoefficient(ZpPoly zpPoly, int pointNum, int expectNum, BigInteger[] coefficients) {
        Assert.assertEquals(zpPoly.coefficientNum(pointNum, expectNum), coefficients.length);
        Arrays.stream(coefficients).forEach(zpPoly::validPoint);
    }

    private void assertEvaluate(ZpPoly zpPoly, BigInteger[] coefficients, BigInteger[] xArray, BigInteger[] yArray) {
        // 逐一求值
        IntStream.range(0, xArray.length).forEach(index -> {
            BigInteger evaluation = zpPoly.evaluate(coefficients, xArray[index]);
            Assert.assertEquals(yArray[index], evaluation);
        });
        // 批量求值
        BigInteger[] evaluations = zpPoly.evaluate(coefficients, xArray);
        Assert.assertArrayEquals(yArray, evaluations);
    }

    @Test
    public void testEmptyRootInterpolation() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, DEFAULT_L);
        BigInteger p = zpPoly.getPrime();
        int pointNum = 0;
        BigInteger[] xArray = new BigInteger[pointNum];
        BigInteger y = BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM);
        // 没有插值点，但要补充随机点
        BigInteger[] coefficients = zpPoly.rootInterpolate(DEFAULT_NUM, xArray, y);
        assertRootCoefficient(zpPoly, pointNum, DEFAULT_NUM, coefficients);
    }

    @Test
    public void testOneRootInterpolation() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, DEFAULT_L);
        BigInteger p = zpPoly.getPrime();
        int pointNum = 1;
        // 只存在一组插值点，也应该可以构建多项式
        BigInteger[] xArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger y = BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM);
        // 只有1组插值点
        BigInteger[] coefficients = zpPoly.rootInterpolate(pointNum, xArray, y);
        assertRootCoefficient(zpPoly, pointNum, pointNum, coefficients);
        assertRootEvaluate(zpPoly, coefficients, xArray, y);
        // 只有1组插值点，但要补充随机点
        coefficients = zpPoly.rootInterpolate(DEFAULT_NUM, xArray, y);
        assertRootCoefficient(zpPoly, pointNum, DEFAULT_NUM, coefficients);
        assertRootEvaluate(zpPoly, coefficients, xArray, y);
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
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, l);
        BigInteger p = zpPoly.getPrime();
        int pointNum = DEFAULT_NUM / 2;
        BigInteger[] xArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger y = BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM);
        // 插值一半的点
        BigInteger[] coefficients = zpPoly.rootInterpolate(pointNum, xArray, y);
        assertRootCoefficient(zpPoly, pointNum, pointNum, coefficients);
        assertRootEvaluate(zpPoly, coefficients, xArray, y);
        // 插值一半的点，补充随机点
        coefficients = zpPoly.rootInterpolate(DEFAULT_NUM, xArray, y);
        assertRootCoefficient(zpPoly, pointNum, DEFAULT_NUM, coefficients);
        assertRootEvaluate(zpPoly, coefficients, xArray, y);
    }

    @Test
    public void testRootParallel() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, DEFAULT_L);
        BigInteger p = zpPoly.getPrime();
        ArrayList<BigInteger[]> xArrayList = new ArrayList<>();
        ArrayList<BigInteger> yList = new ArrayList<>();
        IntStream.range(0, MAX_PARALLEL).forEach(parallelIndex -> {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger y = BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM);
            xArrayList.add(xArray);
            yList.add(y);
        });
        IntStream.range(0, MAX_PARALLEL).parallel().forEach(parallelIndex -> {
            BigInteger[] xArray = xArrayList.get(parallelIndex);
            BigInteger y = yList.get(parallelIndex);
            BigInteger[] coefficients = zpPoly.rootInterpolate(DEFAULT_NUM, xArray, y);
            assertRootEvaluate(zpPoly, coefficients, xArray, y);
        });
    }

    private void assertRootCoefficient(ZpPoly zpPoly, int pointNum, int expectNum, BigInteger[] coefficients) {
        Assert.assertEquals(zpPoly.rootCoefficientNum(pointNum, expectNum), coefficients.length);
        Arrays.stream(coefficients).forEach(zpPoly::validPoint);
    }

    private void assertRootEvaluate(ZpPoly zpPoly, BigInteger[] coefficients, BigInteger[] xArray, BigInteger y) {
        // 逐一求值
        Arrays.stream(xArray)
            .map(x -> zpPoly.evaluate(coefficients, x))
            .forEach(evaluation -> Assert.assertEquals(y, evaluation));
        // 批量求值
        Arrays.stream(zpPoly.evaluate(coefficients, xArray))
            .forEach(evaluation -> Assert.assertEquals(y, evaluation));
    }
}
