package edu.alibaba.mpc4j.common.structure.lpn.dual.expander;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.lpn.LpnCoderTestUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * non-systematic expander coder test.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
@RunWith(Parameterized.class)
public class NonSysExpanderCoderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        int logK = 18;
        int k = (1 << logK);
        // EACode parameters
        int scalar = 5;
        configurations.add(new Object[] {"EA 7 (k = 2^" + logK + ")", k, scalar * k, 7});
        configurations.add(new Object[] {"EA 11 (k = 2^" + logK + ")", k, scalar * k, 11});
        configurations.add(new Object[] {"EA 21 (k = 2^" + logK + ")", k, scalar * k, 21});
        configurations.add(new Object[] {"EA 41 (k = 2^" + logK + ")", k, scalar * k, 40});

        return configurations;
    }

    /**
     * k
     */
    private final int k;
    /**
     * n
     */
    private final int n;
    /**
     * expander weight
     */
    private final int expanderWeight;
    /**
     * expander coder
     */
    private final NonSysExpanderCoder expanderCoder;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * R0 Array
     */
    private final byte[][] r0Array;
    /**
     * choices
     */
    private final boolean[] choices;
    /**
     * Rb array
     */
    private final byte[][] rbArray;

    public NonSysExpanderCoderTest(String name, int k, int n, int expanderWeight) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.k = k;
        this.n = n;
        this.expanderWeight = expanderWeight;
        expanderCoder = new NonSysExpanderCoder(k, n, expanderWeight);
        // generate COT: (R0, Δ), (b, Rb)
        SecureRandom secureRandom = new SecureRandom();
        delta = BlockUtils.randomBlock(secureRandom);
        r0Array = LpnCoderTestUtils.generateR0Array(n, secureRandom);
        choices = LpnCoderTestUtils.generateChoices(n, secureRandom);
        rbArray = LpnCoderTestUtils.generateRbArray(delta, r0Array, choices);
    }

    @Test
    public void testParameters() {
        Assert.assertEquals(k, expanderCoder.getMessageSize());
        Assert.assertEquals(n, expanderCoder.getCodeSize());

        int[][] matrix = expanderCoder.getMatrix();
        // number of 1's in rows
        for (int i = 0; i < expanderCoder.getMessageSize(); i++) {
            Assert.assertEquals(expanderWeight, matrix[i].length);
        }
    }

    @Test
    public void testEncode() {
        testEncode(false);
    }

    @Test
    public void testParallelEncode() {
        testEncode(true);
    }

    private void testEncode(boolean parallel) {
        expanderCoder.setParallel(parallel);
        // first encode
        byte[][] extendR0Array = expanderCoder.dualEncode(r0Array);
        byte[][] extendRbArray = expanderCoder.dualEncode(rbArray);
        boolean[] extendChoices = expanderCoder.dualEncode(choices);
        LpnCoderTestUtils.assertEncode(delta, extendR0Array, extendChoices, extendRbArray);
        // second encode
        Assert.assertArrayEquals(extendR0Array, expanderCoder.dualEncode(r0Array));
        Assert.assertArrayEquals(extendChoices, expanderCoder.dualEncode(choices));
        Assert.assertArrayEquals(extendRbArray, expanderCoder.dualEncode(rbArray));
    }
}
