package edu.alibaba.mpc4j.common.tool.galoisfield.zn;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zn.ZnFactory.ZnType;
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
 * Zn tests.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
@RunWith(Parameterized.class)
public class ZnTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        ZnType[] types = new ZnType[]{ZnType.JDK};
        long[] ns = new long[]{2, 3, 4, 7, 8, 247, 350, 511, 512, 513, 701, 833, 991, 1023, 1024, 1025};
        for (ZnType type : types) {
            // add each n
            for (long n : ns) {
                configurations.add(new Object[]{type.name() + ", n = " + n, type, BigInteger.valueOf(n)});
            }
        }

        return configurations;
    }

    /**
     * the type
     */
    private final ZnType type;
    /**
     * the Zn instance
     */
    private final Zn zn;

    public ZnTest(String name, ZnType type, BigInteger n) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        zn = ZnFactory.createInstance(EnvType.STANDARD, type, n);
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, zn.getZnType());
    }

    @Test
    public void testElementBitLength() {
        int elementBitLength = zn.getElementBitLength();
        int l = zn.getL();
        Assert.assertEquals(elementBitLength, l + 1);
    }

    @Test
    public void testElementByteLength() {
        int elementByteLength = zn.getElementByteLength();
        int byteL = zn.getByteL();
        int l = zn.getL();
        if (l % Byte.SIZE == 0) {
            // if l % Byte.SIZE == 0, then elementByteLength = byteL + 1
            Assert.assertEquals(elementByteLength, byteL + 1);
        } else {
            Assert.assertEquals(elementByteLength, byteL);
        }
    }

    @Test
    public void testModulus() {
        // 0 mod n = 0
        Assert.assertEquals(BigInteger.ZERO, zn.module(BigInteger.ZERO));
        // 1 mod n = 1
        Assert.assertEquals(BigInteger.ONE, zn.module(BigInteger.ONE));
        // n + 0 mod n = 0
        Assert.assertEquals(BigInteger.ZERO, zn.module(zn.getN()));
        // n + 1 mod n = 1
        Assert.assertEquals(BigInteger.ONE, zn.module(zn.getN().add(BigInteger.ONE)));
        // -1 mod n = n - 1
        Assert.assertEquals(zn.getN().subtract(BigInteger.ONE), zn.module(BigInteger.ONE.negate()));
        // 1 - n mod n = 1
        Assert.assertEquals(BigInteger.ONE, zn.module(BigInteger.ONE.subtract(zn.getN())));
    }

    @Test
    public void testConstantAddNegSub() {
        BigInteger one = BigInteger.ONE;
        BigInteger two = BigInteger.valueOf(2);
        BigInteger four = BigInteger.valueOf(4);
        BigInteger n = zn.getN();
        if (BigIntegerUtils.greater(n, two)) {
            // 1 + 1 = 2
            Assert.assertEquals(two, zn.add(one, one));
            // -1 = n - 1
            Assert.assertEquals(n.subtract(one), zn.neg(one));
            // 2 - 1 = 1
            Assert.assertEquals(one, zn.sub(two, one));
        }
        if (BigIntegerUtils.greater(n, four)) {
            // 2 + 2 = 4
            Assert.assertEquals(four, zn.add(two, two));
            // -2 = prime - 2
            Assert.assertEquals(n.subtract(two), zn.neg(two));
            // 4 - 2 = 2
            Assert.assertEquals(two, zn.sub(four, two));
        }
    }

    @Test
    public void testConstantMul() {
        BigInteger two = BigInteger.valueOf(2);
        BigInteger four = BigInteger.valueOf(4);
        BigInteger n = zn.getN();
        if (BigIntegerUtils.greater(n ,four)) {
            // 2 * 2 = 4
            Assert.assertEquals(four, zn.mul(two, two));
        }
    }
}
