package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
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
 * private Zl edaBit vector test.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
@RunWith(Parameterized.class)
public class PlainZlEdaBitVectorTest {
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 64;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        Zl[] zls = new Zl[]{
            ZlFactory.createInstance(EnvType.STANDARD, 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L - 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L + 1),
            ZlFactory.createInstance(EnvType.STANDARD, CommonConstants.BLOCK_BIT_LENGTH),
        };
        for (Zl zl : zls) {
            int l = zl.getL();
            configurations.add(new Object[]{"l = " + l + ")", zl,});
        }

        return configurations;
    }

    /**
     * Zl instance
     */
    private final Zl zl;

    public PlainZlEdaBitVectorTest(String name, Zl zl) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.zl = zl;
    }

    @Test
    public void testCreateRandom() {
        int num = DEFAULT_NUM;
        PlainZlEdaBitVector edaBitVector = PlainZlEdaBitVector.createRandom(zl, num, SECURE_RANDOM);
        assertCorrectness(num, edaBitVector);
    }

    @Test
    public void testEmpty() {
        int num = 0;
        PlainZlEdaBitVector edaBitVector = PlainZlEdaBitVector.createEmpty(zl);
        assertCorrectness(num, edaBitVector);
    }



    private void assertCorrectness(int num, PlainZlEdaBitVector edaBitVector) {
        Assert.assertEquals(num, edaBitVector.getNum());
        if (num > 0) {
            for (int index = 0; index < num; index++) {
                BigInteger zlElement = edaBitVector.getZlElement(index);
                BigInteger z2Element = edaBitVector.getZ2Element(index);
                Assert.assertEquals(zlElement, z2Element);
            }
        }
    }
}
