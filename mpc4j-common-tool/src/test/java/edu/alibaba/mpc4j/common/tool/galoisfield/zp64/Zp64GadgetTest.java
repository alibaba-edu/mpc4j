package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64Gadget;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

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
        configurations.add(new Object[]{"l = 1", 1});
        configurations.add(new Object[]{"l = 2", 2});
        configurations.add(new Object[]{"l = 62", 62});

        return configurations;
    }

    /**
     * Zp64
     */
    private final Zp64 zp64;
    /**
     * Zp小工具
     */
    private final Zp64Gadget gadget;

    public Zp64GadgetTest(String name, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        zp64 = Zp64Factory.createInstance(EnvType.STANDARD, l);
        gadget = new Zp64Gadget(zp64);
    }

    @Test
    public void testZp64Gadget() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            // 随机生成[0, 2^k)范围内的元素
            long rangeElement = zp64.createRangeRandom(SECURE_RANDOM);
            boolean[] decomposition = gadget.decomposition(rangeElement);
            long compositeRangeElement = gadget.composition(decomposition);
            Assert.assertEquals(rangeElement, compositeRangeElement);
        }
    }
}
