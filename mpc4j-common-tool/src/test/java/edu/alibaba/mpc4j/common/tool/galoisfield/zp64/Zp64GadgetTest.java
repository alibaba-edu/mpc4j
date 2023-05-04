package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Zp64小工具测试。
 *
 * @author Hanwen Feng
 * @date 2022/06/14
 */
@RunWith(Parameterized.class)
public class Zp64GadgetTest {
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 随机测试轮数
     */
    private static final int RANDOM_ROUND = 40;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 61, 62};
        for (int l : ls) {
            configurations.add(new Object[]{"l = " + l, l});
        }

        return configurations;
    }

    /**
     * Zp64
     */
    private final Zp64 zp64;
    /**
     * Zp小工具
     */
    private final Zp64Gadget zp64Gadget;

    public Zp64GadgetTest(String name, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        zp64 = Zp64Factory.createInstance(EnvType.STANDARD, l);
        zp64Gadget = new Zp64Gadget(zp64);
    }

    @Test
    public void testBitComposition() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            // 随机生成[0, 2^k)范围内的元素
            long rangeElement = zp64.createRangeRandom(SECURE_RANDOM);
            boolean[] decomposition = zp64Gadget.bitDecomposition(rangeElement);
            long compositeRangeElement = zp64Gadget.bitComposition(decomposition);
            Assert.assertEquals(rangeElement, compositeRangeElement);
        }
    }

    @Test
    public void testInnerProduct() {
        int l = zp64.getL();
        // 全为0的内积为0
        long[] zeroElementVector = IntStream.range(0, l)
            .mapToLong(index -> 0L)
            .toArray();
        long zeroInnerProduct = zp64Gadget.innerProduct(zeroElementVector);
        Assert.assertEquals(0L, zeroInnerProduct);
        // 全为1的内积为-1
        long negOne = (1L << l) - 1;
        long[] oneElementVector = IntStream.range(0, l)
            .mapToLong(index -> 1L)
            .toArray();
        long oneInnerProduct = zp64Gadget.innerProduct(oneElementVector);
        Assert.assertEquals(negOne, oneInnerProduct);
    }
}
