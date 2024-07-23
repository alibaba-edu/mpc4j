package edu.alibaba.mpc4j.common.structure.lpn.dual.excoder;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.lpn.LpnCoderTestUtils;
import edu.alibaba.mpc4j.common.structure.lpn.dual.excoder.ExCoderFactory.ExCoderType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * tests for Expand-Accumulator coder and Expand-Cunvolute coder.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
@RunWith(Parameterized.class)
public class ExCoderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (ExCoderType type : ExCoderType.values()) {
            configurations.add(new Object[] {type.name(), type});
        }


        return configurations;
    }

    /**
     * coder
     */
    private final ExCoder coder;
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

    public ExCoderTest(String name, ExCoderType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        int k = 1 << 18;
        coder = ExCoderFactory.createExCoder(type, k);
        // generate COT: (R0, Δ), (b, Rb)
        SecureRandom secureRandom = new SecureRandom();
        int n = ExCoderFactory.getScalar(type) * k;
        delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        r0Array = LpnCoderTestUtils.generateR0Array(n, secureRandom);
        choices = LpnCoderTestUtils.generateChoices(n, secureRandom);
        rbArray = LpnCoderTestUtils.generateRbArray(delta, r0Array, choices);
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
        coder.setParallel(parallel);
        // first encode
        byte[][] extendR0Array = coder.dualEncode(r0Array);
        byte[][] extendRbArray = coder.dualEncode(rbArray);
        boolean[] extendChoices = coder.dualEncode(choices);
        LpnCoderTestUtils.assertEncode(delta, extendR0Array, extendChoices, extendRbArray);
        // second encode
        Assert.assertArrayEquals(extendR0Array, coder.dualEncode(r0Array));
        Assert.assertArrayEquals(extendChoices, coder.dualEncode(choices));
        Assert.assertArrayEquals(extendRbArray, coder.dualEncode(rbArray));
    }
}
