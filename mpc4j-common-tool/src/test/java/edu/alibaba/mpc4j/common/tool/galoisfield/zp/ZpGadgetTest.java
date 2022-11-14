package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Zp小工具测试。
 *
 * @author Hanwen Feng
 * @date 2022/6/13
 */
@RunWith(Parameterized.class)
public class ZpGadgetTest {
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
        configurations.add(new Object[]{"l = 1", 1});
        configurations.add(new Object[]{"l = 2", 2});
        configurations.add(new Object[]{"l = 63", 63});
        configurations.add(new Object[]{"l = 64", 64});
        configurations.add(new Object[]{"l = 65", 65});
        configurations.add(new Object[]{"l = 127", 127});
        configurations.add(new Object[]{"l = 128", 128});
        configurations.add(new Object[]{"l = 129", 129});

        return configurations;
    }

    /**
     * Zp
     */
    private final Zp zp;
    /**
     * Zp小工具
     */
    private final ZpGadget zpGadget;

    public ZpGadgetTest(String name, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        zp = ZpFactory.createInstance(EnvType.STANDARD, l);
        zpGadget = new ZpGadget(zp);
    }

    @Test
    public void testBitComposition() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            // 随机生成[0, 2^k)范围内的元素
            BigInteger element = zp.createRangeRandom(SECURE_RANDOM);
            boolean[] decomposition = zpGadget.decomposition(element);
            BigInteger compositeElement = zpGadget.composition(decomposition);
            Assert.assertEquals(element, compositeElement);
        }
    }

    @Test
    public void testInnerProduct() {
        int l = zp.getL();
        // 全为0的内积为0
        BigInteger[] zeroElementVector = IntStream.range(0, l)
            .mapToObj(index -> BigInteger.ZERO)
            .toArray(BigInteger[]::new);
        BigInteger zeroInnerProduct = zpGadget.innerProduct(zeroElementVector);
        Assert.assertEquals(BigInteger.ZERO, zeroInnerProduct);
        // 全为1的内积为-1
        BigInteger negOne = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        BigInteger[] oneElementVector = IntStream.range(0, l)
            .mapToObj(index -> BigInteger.ONE)
            .toArray(BigInteger[]::new);
        BigInteger oneInnerProduct = zpGadget.innerProduct(oneElementVector);
        Assert.assertEquals(negOne, oneInnerProduct);
    }
}
