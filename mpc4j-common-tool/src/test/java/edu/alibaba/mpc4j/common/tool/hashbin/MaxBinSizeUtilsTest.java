package edu.alibaba.mpc4j.common.tool.hashbin;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 最大桶数量计算测试。
 *
 * @author Weiran Liu
 * @date 2021/12/16
 */
@RunWith(Parameterized.class)
public class MaxBinSizeUtilsTest {

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configuration = new ArrayList<>();
        // n = 2^7, b = 1
        configuration.add(new Object[] {"n = 2^7, b = 1", 1 << 7, 1, });
        // n = 2^7, b = 2
        configuration.add(new Object[] {"n = 2^7, b = 2", 1 << 7, 2, });
        // n = 2^7, b = 128
        configuration.add(new Object[] {"n = 2^7, b = 128", 1 << 7, 1 << 7, });
        // n = 2^10, b = 10
        configuration.add(new Object[] {"n = 2^10, b = 5", 1 << 10, 5, });
        // n = 2^10, b = 20
        configuration.add(new Object[] {"n = 2^10, b = 10", 1 << 10, 10, });

        return configuration;
    }

    /**
     * 元素数量
     */
    private final int n;
    /**
     * 桶数量
     */
    private final int b;

    public MaxBinSizeUtilsTest(String name, int n, int b) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.n = n;
        this.b = b;
    }

    @Test
    public void testMaxBinSize() {
        int expectK = MaxBinSizeUtils.expectMaxBinSize(n, b);
        int exactK = MaxBinSizeUtils.exactMaxBinSize(n, b);
        Assert.assertTrue(exactK >= ((double)n / b) && expectK >= exactK);
    }
}
