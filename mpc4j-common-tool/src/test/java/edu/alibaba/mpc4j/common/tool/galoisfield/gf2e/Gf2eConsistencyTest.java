package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * GF(2^l)运算一致性测试。
 *
 * @author Weiran Liu
 * @date 2022/5/19
 */
@RunWith(Parameterized.class)
@Ignore
public class Gf2eConsistencyTest {
    /**
     * 随机测试轮数
     */
    private static final int MAX_RANDOM_ROUND = 1000;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // Rings V.S. NTL
        configurationParams.add(new Object[] {"Rings V.S. NTL (l = 1)", Gf2eType.RINGS, Gf2eType.NTL, 1});
        configurationParams.add(new Object[] {"Rings V.S. NTL (l = 2)", Gf2eType.RINGS, Gf2eType.NTL, 2});
        configurationParams.add(new Object[] {"Rings V.S. NTL (l = 3)", Gf2eType.RINGS, Gf2eType.NTL, 3});
        configurationParams.add(new Object[] {"Rings V.S. NTL (l = 4)", Gf2eType.RINGS, Gf2eType.NTL, 4});
        configurationParams.add(new Object[] {"Rings V.S. NTL (l = 39)", Gf2eType.RINGS, Gf2eType.NTL, 39});
        configurationParams.add(new Object[] {"Rings V.S. NTL (l = 40)", Gf2eType.RINGS, Gf2eType.NTL, 40});
        configurationParams.add(new Object[] {"Rings V.S. NTL (l = 41)", Gf2eType.RINGS, Gf2eType.NTL, 41});
        configurationParams.add(new Object[] {"Rings V.S. NTL (l = 128)", Gf2eType.RINGS, Gf2eType.NTL, 128});
        configurationParams.add(new Object[] {"Rings V.S. NTL (l = 256)", Gf2eType.RINGS, Gf2eType.NTL, 256});

        return configurationParams;
    }

    /**
     * 被比较类型
     */
    private final Gf2e thisGf2e;
    /**
     * 比较类型
     */
    private final Gf2e thatGf2e;

    public Gf2eConsistencyTest(String name, Gf2eType thisType, Gf2eType thatType, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        thisGf2e = Gf2eFactory.createInstance(EnvType.STANDARD, thisType, l);
        thatGf2e = Gf2eFactory.createInstance(EnvType.STANDARD, thatType, l);
        Assert.assertEquals(thisGf2e.getL(), thatGf2e.getL());
        Assert.assertEquals(thisGf2e.getByteL(), thatGf2e.getByteL());
    }

    @Test
    public void testMulConsistency() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] a = thisGf2e.createRandom(SECURE_RANDOM);
            byte[] b = thisGf2e.createRandom(SECURE_RANDOM);
            byte[] thisResult = thisGf2e.mul(a, b);
            byte[] thatResult = thatGf2e.mul(a, b);
            Assert.assertArrayEquals(thisResult, thatResult);
        }
    }

    @Test
    public void testMuliConsistency() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] thisA = thisGf2e.createRandom(SECURE_RANDOM);
            byte[] thatA = BytesUtils.clone(thisA);
            byte[] b = thisGf2e.createRandom(SECURE_RANDOM);
            thisGf2e.muli(thisA, b);
            thatGf2e.muli(thatA, b);
            Assert.assertArrayEquals(thisA, thatA);
        }
    }
}
