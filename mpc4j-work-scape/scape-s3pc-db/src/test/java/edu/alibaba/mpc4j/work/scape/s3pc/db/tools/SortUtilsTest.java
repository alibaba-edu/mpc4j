package edu.alibaba.mpc4j.work.scape.s3pc.db.tools;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * test cases for SortUtils
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class SortUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SortUtilsTest.class);

    @Test
    public void testExample() {
        // 示例输入
        BigInteger[] x = {
            new BigInteger("10"),
            new BigInteger("3"),
            new BigInteger("7"),
            new BigInteger("1")
        };

        // 调用函数计算置换数组
        int[] pi = SortUtils.getPermutation(x);

        // 输出结果
        LOGGER.info("input x: {}", Arrays.toString(x));
        LOGGER.info("output pi: {}", Arrays.toString(pi));
    }

    @Test
    public void testRandom() {
        BigInteger[] x = IntStream.range(0, 1 << 10)
            .mapToObj(i -> BigIntegerUtils.randomNonNegative(BigInteger.valueOf(1 << 8), new SecureRandom()))
            .toArray(BigInteger[]::new);

        int[] pi = SortUtils.getPermutation(x);

        PermutationNetworkUtils.validPermutation(pi);
        for (int i = 1; i < x.length; i++) {
            Assert.assertTrue(x[pi[i]].compareTo(x[pi[i - 1]]) >= 0);
        }
    }
}
