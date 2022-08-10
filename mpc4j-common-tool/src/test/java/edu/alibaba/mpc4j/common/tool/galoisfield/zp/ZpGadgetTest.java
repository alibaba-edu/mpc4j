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
        configurations.add(new Object[]{"k = 1", 1});
        configurations.add(new Object[]{"k = 2", 2});
        configurations.add(new Object[]{"k = 63", 63});
        configurations.add(new Object[]{"k = 64", 64});
        configurations.add(new Object[]{"k = 128", 65});

        return configurations;
    }

    /**
     * k
     */
    private final int k;
    /**
     * Zp小工具
     */
    private final ZpGadget zpGadget;

    public ZpGadgetTest(String name, int k) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.k = k;
        this.zpGadget = ZpGadget.createFromK(EnvType.STANDARD, k);
    }

    @Test
    public void testZpGadget() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            // 随机生成[0, 2^k)范围内的元素
            BigInteger randomElement = new BigInteger(k, SECURE_RANDOM);
            boolean[] decomposition = zpGadget.decomposition(randomElement);
            BigInteger compositeRandomElement = zpGadget.composition(decomposition);
            Assert.assertEquals(randomElement, compositeRandomElement);
        }
    }
}
