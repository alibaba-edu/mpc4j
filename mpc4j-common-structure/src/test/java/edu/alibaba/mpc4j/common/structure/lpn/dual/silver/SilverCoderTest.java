package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.lpn.LpnCoderTestUtils;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
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
 * Silver Coder test.
 *
 * @author Hanwen Feng
 * @date 2022/3/21
 */
@RunWith(Parameterized.class)
public class SilverCoderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int ceilLogK = 12; ceilLogK < 20; ceilLogK++) {
            configurations.add(new Object[] {"Silver5, k = 2^" + ceilLogK, SilverCodeType.SILVER_5, ceilLogK});
            configurations.add(new Object[] {"Silver11, k = 2^" + ceilLogK, SilverCodeType.SILVER_11, ceilLogK});
        }
        return configurations;
    }

    /**
     * Silver Code type
     */
    private final SilverCodeType silverCodeType;
    /**
     * Silver Coder
     */
    private final SilverCoder silverCoder;
    /**
     * LPN parameters
     */
    private final LpnParams lpnParams;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public SilverCoderTest(String name, SilverCodeType silverCodeType, int ceilLogN) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.silverCodeType = silverCodeType;
        SilverCodeCreator creator = SilverCodeCreatorFactory.createInstance(silverCodeType, ceilLogN);
        silverCoder = creator.createCoder();
        lpnParams = creator.getLpnParams();
        secureRandom = new SecureRandom();
    }

    @Test
    public void testParameters() {
        int gap = SilverCodeCreatorUtils.getGap(silverCodeType);
        int k = lpnParams.getK();
        int n = 2 * k - gap;
        Assert.assertEquals(k, silverCoder.getMessageSize());
        Assert.assertEquals(n, silverCoder.getCodeSize());
    }

    @Test
    public void testEncode() {
        // generate COT: (R0, Δ), (b, Rb)
        int n = silverCoder.getCodeSize();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        byte[][] r0Array = LpnCoderTestUtils.generateR0Array(n, secureRandom);
        boolean[] choices = LpnCoderTestUtils.generateChoices(n, secureRandom);
        byte[][] rbArray = LpnCoderTestUtils.generateRbArray(delta, r0Array, choices);
        // first encode
        byte[][] extendR0Array = silverCoder.dualEncode(r0Array);
        byte[][] extendRbArray = silverCoder.dualEncode(rbArray);
        boolean[] extendChoices = silverCoder.dualEncode(choices);
        LpnCoderTestUtils.assertEncode(delta, extendR0Array, extendChoices, extendRbArray);
        // second encode
        Assert.assertArrayEquals(extendR0Array, silverCoder.dualEncode(r0Array));
        Assert.assertArrayEquals(extendChoices, silverCoder.dualEncode(choices));
        Assert.assertArrayEquals(extendRbArray, silverCoder.dualEncode(rbArray));
    }
}
