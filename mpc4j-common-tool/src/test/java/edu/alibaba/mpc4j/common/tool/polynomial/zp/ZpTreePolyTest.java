package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import com.google.common.base.Preconditions;
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
 * Zp二叉树多项式插值测试。
 *
 * @author Weiran Liu
 * @date 2022/11/5
 */
@RunWith(Parameterized.class)
public class ZpTreePolyTest {
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
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // NTL_TREE
        configurations.add(new Object[]{
            ZpPolyFactory.ZpTreePolyType.NTL_TREE.name(), ZpPolyFactory.ZpTreePolyType.NTL_TREE,}
        );
        // RINGS_TREE
        configurations.add(new Object[]{
            ZpPolyFactory.ZpTreePolyType.RINGS_TREE.name(), ZpPolyFactory.ZpTreePolyType.RINGS_TREE,}
        );

        return configurations;
    }

    private final ZpPolyFactory.ZpTreePolyType type;

    public ZpTreePolyTest(String name, ZpPolyFactory.ZpTreePolyType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        ZpTreePoly zpTreePoly = ZpPolyFactory.createTreeInstance(type, DEFAULT_L);
        Assert.assertEquals(type, zpTreePoly.getType());
    }

    @Test
    public void testIllegalInputs() {
        // 尝试设置l = 0
        try {
            ZpPolyFactory.createTreeInstance(type, 0);
            throw new IllegalStateException("ERROR: successfully create ZpTreePoly with l = 0");
        } catch (AssertionError ignored) {

        }
        ZpTreePoly zpTreePoly = ZpPolyFactory.createTreeInstance(type, DEFAULT_L);
        BigInteger p = zpTreePoly.getPrime();
        // 尝试对数量不匹配的点插值
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            zpTreePoly.prepareInterpolateBinaryTree(xArray);
            zpTreePoly.interpolate(yArray);
            throw new IllegalStateException("ERROR: successfully interpolate with x.length < y.length");
        } catch (AssertionError ignored) {

        }
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            zpTreePoly.prepareInterpolateBinaryTree(xArray);
            zpTreePoly.interpolate(yArray);
            throw new IllegalStateException("ERROR: successfully interpolate with x.length > y.length");
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
            zpTreePoly.prepareInterpolateBinaryTree(xArray);
            zpTreePoly.interpolate(yArray);
            throw new IllegalStateException("ERROR: successfully interpolate large values");
        } catch (AssertionError ignored) {

        }
        // 尝试对数量不匹配的点求值
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            zpTreePoly.prepareInterpolateBinaryTree(xArray);
            BigInteger[] coefficients = zpTreePoly.interpolate(yArray);
            zpTreePoly.prepareEvaluateBinaryTrees(xArray.length - 1, xArray);
            zpTreePoly.evaluate(coefficients);
            throw new IllegalStateException("ERROR: successfully evaluate with large polynomial");
        } catch (AssertionError ignored) {

        }
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            zpTreePoly.prepareInterpolateBinaryTree(xArray);
            BigInteger[] coefficients = zpTreePoly.interpolate(yArray);
            zpTreePoly.prepareEvaluateBinaryTrees(xArray.length + 1, xArray);
            zpTreePoly.evaluate(coefficients);
            throw new IllegalStateException("ERROR: successfully evaluate with small polynomial");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testOneInterpolation() {
        ZpTreePoly zpTreePoly = ZpPolyFactory.createTreeInstance(type, DEFAULT_L);
        BigInteger p = zpTreePoly.getPrime();
        int pointNum = 1;
        BigInteger[] xArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        // 只有1组插值点
        zpTreePoly.prepareInterpolateBinaryTree(xArray);
        BigInteger[] coefficients = zpTreePoly.interpolate(yArray);
        zpTreePoly.destroyInterpolateBinaryTree();
        assertCoefficient(zpTreePoly, pointNum, coefficients);
        assertEvaluate(zpTreePoly, coefficients, xArray, yArray);
    }

    @Test
    public void testConstantInterpolation() {
        for (int l : L_ARRAY) {
            testConstantInterpolation(l);
        }
    }

    private void testConstantInterpolation(int l) {
        ZpTreePoly zpTreePoly = ZpPolyFactory.createTreeInstance(type, l);
        BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(BigInteger::valueOf)
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(BigInteger::valueOf)
            .toArray(BigInteger[]::new);
        zpTreePoly.prepareInterpolateBinaryTree(xArray);
        BigInteger[] coefficients = zpTreePoly.interpolate(yArray);
        zpTreePoly.destroyInterpolateBinaryTree();
        assertCoefficient(zpTreePoly, DEFAULT_NUM, coefficients);
        assertEvaluate(zpTreePoly, coefficients, xArray, yArray);
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
        ZpTreePoly zpTreePoly = ZpPolyFactory.createTreeInstance(type, l);
        BigInteger p = zpTreePoly.getPrime();
        BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(p, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        zpTreePoly.prepareInterpolateBinaryTree(xArray);
        BigInteger[] coefficients = zpTreePoly.interpolate(yArray);
        zpTreePoly.destroyEvaluateBinaryTree();
        assertCoefficient(zpTreePoly, DEFAULT_NUM, coefficients);
        assertEvaluate(zpTreePoly, coefficients, xArray, yArray);
    }

    private void assertCoefficient(ZpTreePoly zpTreePoly, int pointNum, BigInteger[] coefficients) {
        Assert.assertEquals(zpTreePoly.coefficientNum(pointNum), coefficients.length);
        Arrays.stream(coefficients).forEach(zpTreePoly::validPoint);
    }

    private void assertEvaluate(ZpTreePoly zpTreePoly, BigInteger[] coefficients, BigInteger[] xArray, BigInteger[] yArray) {
        // 对所有点求值
        zpTreePoly.prepareEvaluateBinaryTrees(xArray.length, xArray);
        BigInteger[] evaluations = zpTreePoly.evaluate(coefficients);
        Assert.assertArrayEquals(yArray, evaluations);
        zpTreePoly.destroyEvaluateBinaryTree();
        // 不同数量的点求值
        BigInteger[] xOtherArray;
        BigInteger[] yOtherArray;
        // 对一半减1的点求值
        if (xArray.length / 2 - 1 > 0) {
            xOtherArray = new BigInteger[xArray.length / 2 - 1];
            System.arraycopy(xArray, 0, xOtherArray, 0, xOtherArray.length);
            yOtherArray = new BigInteger[yArray.length / 2 - 1];
            System.arraycopy(yArray, 0, yOtherArray, 0, yOtherArray.length);
            zpTreePoly.prepareEvaluateBinaryTrees(xArray.length, xOtherArray);
            evaluations = zpTreePoly.evaluate(coefficients);
            Assert.assertArrayEquals(yOtherArray, evaluations);
            zpTreePoly.destroyEvaluateBinaryTree();
        }
        // 对一半加1的点求值
        xOtherArray = new BigInteger[xArray.length / 2 + 1];
        System.arraycopy(xArray, 0, xOtherArray, 0, xOtherArray.length);
        yOtherArray = new BigInteger[yArray.length / 2 + 1];
        System.arraycopy(yArray, 0, yOtherArray, 0, yOtherArray.length);
        zpTreePoly.prepareEvaluateBinaryTrees(xArray.length, xOtherArray);
        evaluations = zpTreePoly.evaluate(coefficients);
        Assert.assertArrayEquals(yOtherArray, evaluations);
        zpTreePoly.destroyEvaluateBinaryTree();
        // 对一倍减1的点求值
        xOtherArray = new BigInteger[xArray.length * 2 - 1];
        System.arraycopy(xArray, 0, xOtherArray, 0, xArray.length);
        System.arraycopy(xArray, 0, xOtherArray, xArray.length, xOtherArray.length - xArray.length);
        yOtherArray = new BigInteger[yArray.length * 2 - 1];
        System.arraycopy(yArray, 0, yOtherArray, 0, yArray.length);
        System.arraycopy(yArray, 0, yOtherArray, yArray.length, yOtherArray.length - yArray.length);
        zpTreePoly.prepareEvaluateBinaryTrees(xArray.length, xOtherArray);
        evaluations = zpTreePoly.evaluate(coefficients);
        Assert.assertArrayEquals(yOtherArray, evaluations);
        zpTreePoly.destroyEvaluateBinaryTree();
        // 对一倍加1的点求值
        xOtherArray = new BigInteger[xArray.length * 2 + 1];
        System.arraycopy(xArray, 0, xOtherArray, 0, xArray.length);
        System.arraycopy(xArray, 0, xOtherArray, xArray.length, xArray.length);
        System.arraycopy(xArray, 0, xOtherArray, xArray.length * 2, xOtherArray.length - xArray.length * 2);
        yOtherArray = new BigInteger[yArray.length * 2 + 1];
        System.arraycopy(yArray, 0, yOtherArray, 0, yArray.length);
        System.arraycopy(yArray, 0, yOtherArray, yArray.length, yArray.length);
        System.arraycopy(yArray, 0, yOtherArray, yArray.length * 2, yOtherArray.length - yArray.length * 2);
        zpTreePoly.prepareEvaluateBinaryTrees(xArray.length, xOtherArray);
        evaluations = zpTreePoly.evaluate(coefficients);
        Assert.assertArrayEquals(yOtherArray, evaluations);
        zpTreePoly.destroyEvaluateBinaryTree();
    }
}
