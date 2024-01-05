package edu.alibaba.mpc4j.common.structure.okve.tool;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * Zp linear independent row finder test.
 *
 * @author Weiran Liu
 * @date 2021/09/11
 */
public class ZpMaxLisFinderTest {
    /**
     * Zp instance
     */
    private static final Zp ZP = ZpFactory.createInstance(
        EnvType.STANDARD, new BigInteger("A636C49D9AD05B53E5009089BB1BCCE5", 16)
    );

    /**
     * 3-by-3 system, singular
     */
    private static final BigInteger[][] SINGULAR_3_3 = new BigInteger[][]{
        {ZP.module(BigInteger.valueOf(0)), ZP.module(BigInteger.valueOf(1)), ZP.module(BigInteger.valueOf(1))},
        {ZP.module(BigInteger.valueOf(2)), ZP.module(BigInteger.valueOf(4)), ZP.module(BigInteger.valueOf(-2))},
        {ZP.module(BigInteger.valueOf(0)), ZP.module(BigInteger.valueOf(3)), ZP.module(BigInteger.valueOf(15))},
    };
    private static final TIntSet SINGULAR_3_3_RESULT = new TIntHashSet(new int[]{0, 1, 2});

    /**
     * 3-by-3 system, non-singular
     */
    private static final BigInteger[][] NON_SINGULAR_3_3 = new BigInteger[][]{
        {ZP.module(BigInteger.valueOf(1)), ZP.module(BigInteger.valueOf(-3)), ZP.module(BigInteger.valueOf(1))},
        {ZP.module(BigInteger.valueOf(2)), ZP.module(BigInteger.valueOf(-8)), ZP.module(BigInteger.valueOf(8))},
        {ZP.module(BigInteger.valueOf(-6)), ZP.module(BigInteger.valueOf(3)), ZP.module(BigInteger.valueOf(-15))},
    };
    private static final TIntSet NON_SINGULAR_3_3_RESULT = new TIntHashSet(new int[]{0, 1, 2});

    /**
     * 4-by-3 system, singular
     */
    private static final BigInteger[][] SINGULAR_4_3 = new BigInteger[][]{
        {ZP.module(BigInteger.valueOf(0)), ZP.module(BigInteger.valueOf(1)), ZP.module(BigInteger.valueOf(1))},
        {ZP.module(BigInteger.valueOf(2)), ZP.module(BigInteger.valueOf(4)), ZP.module(BigInteger.valueOf(-2))},
        {ZP.module(BigInteger.valueOf(0)), ZP.module(BigInteger.valueOf(3)), ZP.module(BigInteger.valueOf(15))},
        {ZP.module(BigInteger.valueOf(2)), ZP.module(BigInteger.valueOf(8)), ZP.module(BigInteger.valueOf(14))},
    };
    private static final TIntSet SINGULAR_4_3_RESULT = new TIntHashSet(new int[]{0, 1, 2});

    /**
     * binary max linear independent row finder
     */
    private final ZpMaxLisFinder maxLisFinder;

    public ZpMaxLisFinderTest() {
        maxLisFinder = new ZpMaxLisFinder(ZP);
    }

    @Test
    public void testSingular3x3() {
        this.test(SINGULAR_3_3, SINGULAR_3_3_RESULT);
    }

    @Test
    public void testNonSingular3x3() {
        this.test(NON_SINGULAR_3_3, NON_SINGULAR_3_3_RESULT);
    }

    @Test
    public void testSingular4x3() {
        this.test(SINGULAR_4_3, SINGULAR_4_3_RESULT);
    }

    private void test(BigInteger[][] matrix, TIntSet result) {
        TIntSet lisRows = maxLisFinder.getLisRows(matrix);
        Assert.assertEquals(result, lisRows);
    }
}
