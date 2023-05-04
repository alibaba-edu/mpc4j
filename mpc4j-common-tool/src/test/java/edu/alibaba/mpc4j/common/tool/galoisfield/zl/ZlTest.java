package edu.alibaba.mpc4j.common.tool.galoisfield.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory.ZlType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The Zl tests.
 *
 * @author Weiran Liu
 * @date 2023/2/18
 */
@RunWith(Parameterized.class)
public class ZlTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        ZlType[] types = new ZlType[]{ZlType.JDK};
        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 61, 62, 63, 64, 65, 127, 128, 129};
        for (ZlType type : types) {
            // add each l
            for (int l : ls) {
                configurations.add(new Object[]{type.name() + ", l = " + l, type, l});
            }
        }

        return configurations;
    }

    /**
     * The Zl type
     */
    private final ZlType type;
    /**
     * the Zl
     */
    private final Zl zl;

    public ZlTest(String name, ZlType type, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        zl = ZlFactory.createInstance(EnvType.STANDARD, type, l);
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, zl.getZlType());
    }

    @Test
    public void testElementBitLength() {
        int elementBitLength = zl.getElementBitLength();
        int l = zl.getL();
        Assert.assertEquals(elementBitLength, l);
    }

    @Test
    public void testElementByteLength() {
        int elementByteLength = zl.getElementByteLength();
        int byteL = zl.getByteL();
        Assert.assertEquals(elementByteLength, byteL);
    }

    @Test
    public void testModulus() {
        // 0 mod 2^l = 0
        Assert.assertEquals(BigInteger.ZERO, zl.module(BigInteger.ZERO));
        // 1 mod 2^l = 1
        Assert.assertEquals(BigInteger.ONE, zl.module(BigInteger.ONE));
        // 2^l + 0 mod 2^l = 0
        Assert.assertEquals(BigInteger.ZERO, zl.module(zl.getRangeBound()));
        // 2^l + 1 mod 2^l = 1
        Assert.assertEquals(BigInteger.ONE, zl.module(zl.getRangeBound().add(BigInteger.ONE)));
        // -1 mod 2^l = 2^l - 1
        Assert.assertEquals(zl.getRangeBound().subtract(BigInteger.ONE), zl.module(BigInteger.ONE.negate()));
        // 1 - 2^l mod 2^l = 1
        Assert.assertEquals(BigInteger.ONE, zl.module(BigInteger.ONE.subtract(zl.getRangeBound())));
    }

    @Test
    public void testConstantAddNegSub() {
        BigInteger one = BigInteger.ONE;
        BigInteger two = BigInteger.valueOf(2);
        BigInteger four = BigInteger.valueOf(4);
        BigInteger rangeBound = zl.getRangeBound();
        if (BigIntegerUtils.greater(rangeBound, two)) {
            // 1 + 1 = 2
            Assert.assertEquals(two, zl.add(one, one));
            // -1 = 2^l - 1
            Assert.assertEquals(rangeBound.subtract(one), zl.neg(one));
            // 2 - 1 = 1
            Assert.assertEquals(one, zl.sub(two, one));
        }
        if (BigIntegerUtils.greater(rangeBound, four)) {
            // 2 + 2 = 4
            Assert.assertEquals(four, zl.add(two, two));
            // -2 = 2^l - 2
            Assert.assertEquals(rangeBound.subtract(two), zl.neg(two));
            // 4 - 2 = 2
            Assert.assertEquals(two, zl.sub(four, two));
        }
    }

    @Test
    public void testConstantMul() {
        BigInteger two = BigInteger.valueOf(2);
        BigInteger four = BigInteger.valueOf(4);
        BigInteger modulus = zl.getRangeBound();
        if (BigIntegerUtils.greater(modulus, four)) {
            // 2 * 2 = 4
            Assert.assertEquals(four, zl.mul(two, two));
        }
    }
}
