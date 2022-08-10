package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Z_p域最大线性无关组查找器测试类。测试例子来自于：
 * <p>
 * https://algs4.cs.princeton.edu/99scientific/GaussianElimination.java.html
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/09/11
 */
public class ZpMaxLisFinderTest {
    /**
     * 默认的256比特质数
     */
    private static final BigInteger PRIME = new BigInteger("A636C49D9AD05B53E5009089BB1BCCE5", 16);

    /**
     * test 1 (3-by-3 system, non-singular)
     */
    private static final BigInteger[][] TEST_1 = new BigInteger[][]{
        {BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(1).mod(PRIME), BigInteger.valueOf(1).mod(PRIME)},
        {BigInteger.valueOf(2).mod(PRIME), BigInteger.valueOf(4).mod(PRIME), BigInteger.valueOf(-2).mod(PRIME)},
        {BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(3).mod(PRIME), BigInteger.valueOf(15).mod(PRIME)},
    };
    private static final Set<Integer> TEST_1_RESULT = Arrays.stream(new int[]{0, 1, 2})
        .boxed()
        .collect(Collectors.toSet());

    /**
     * test 2 (3-by-3 system, non-singular)
     */
    private static final BigInteger[][] TEST_2 = new BigInteger[][]{
        {BigInteger.valueOf(1).mod(PRIME), BigInteger.valueOf(-3).mod(PRIME), BigInteger.valueOf(1).mod(PRIME)},
        {BigInteger.valueOf(2).mod(PRIME), BigInteger.valueOf(-8).mod(PRIME), BigInteger.valueOf(8).mod(PRIME)},
        {BigInteger.valueOf(-6).mod(PRIME), BigInteger.valueOf(3).mod(PRIME), BigInteger.valueOf(-15).mod(PRIME)},
    };
    private static final Set<Integer> TEST_2_RESULT = Arrays.stream(new int[]{0, 1, 2})
        .boxed()
        .collect(Collectors.toSet());

    /**
     * test 3 (3-by-3 system, non-singular)
     */
    private static final BigInteger[][] TEST_3 = new BigInteger[][]{
        {BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(0).mod(PRIME)},
        {BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(1).mod(PRIME)},
        {BigInteger.valueOf(1).mod(PRIME), BigInteger.valueOf(1).mod(PRIME), BigInteger.valueOf(1).mod(PRIME)},
    };
    private static final Set<Integer> TEST_3_RESULT = Arrays.stream(new int[]{1, 2})
        .boxed()
        .collect(Collectors.toSet());

    /**
     * test 4 (4-by-3 system, singular)
     */
    private static final BigInteger[][] TEST_4 = new BigInteger[][]{
        {BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(1).mod(PRIME), BigInteger.valueOf(1).mod(PRIME)},
        {BigInteger.valueOf(2).mod(PRIME), BigInteger.valueOf(4).mod(PRIME), BigInteger.valueOf(-2).mod(PRIME)},
        {BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(3).mod(PRIME), BigInteger.valueOf(15).mod(PRIME)},
        {BigInteger.valueOf(2).mod(PRIME), BigInteger.valueOf(8).mod(PRIME), BigInteger.valueOf(14).mod(PRIME)},
    };
    private static final Set<Integer> TEST_4_RESULT = Arrays.stream(new int[]{0, 1, 3})
        .boxed()
        .collect(Collectors.toSet());

    /**
     * test 5 (4-by-3 system, singular)
     */
    private static final BigInteger[][] TEST_5 = new BigInteger[][]{
        {BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(1).mod(PRIME), BigInteger.valueOf(1).mod(PRIME)},
        {BigInteger.valueOf(2).mod(PRIME), BigInteger.valueOf(4).mod(PRIME), BigInteger.valueOf(-2).mod(PRIME)},
        {BigInteger.valueOf(0).mod(PRIME), BigInteger.valueOf(3).mod(PRIME), BigInteger.valueOf(15).mod(PRIME)},
        {BigInteger.valueOf(2).mod(PRIME), BigInteger.valueOf(8).mod(PRIME), BigInteger.valueOf(14).mod(PRIME)},
    };
    private static final Set<Integer> TEST_5_RESULT = Arrays.stream(new int[]{0, 1, 3})
        .boxed()
        .collect(Collectors.toSet());

    @Test
    public void test1() {
        this.test(TEST_1, TEST_1_RESULT);
    }

    @Test
    public void test2() {
        this.test(TEST_2, TEST_2_RESULT);
    }

    @Test
    public void test3() {
        this.test(TEST_3, TEST_3_RESULT);
    }

    @Test
    public void test4() {
        this.test(TEST_4, TEST_4_RESULT);
    }

    @Test
    public void test5() {
        this.test(TEST_5, TEST_5_RESULT);
    }

    private void test(BigInteger[][] matrix, Set<Integer> result) {
        ZpMaxLisFinder maxLisFinder = new ZpMaxLisFinder(PRIME, matrix);
        Set<Integer> lisRows = maxLisFinder.getLisRows();
        Assert.assertEquals(result, lisRows);
    }
}
