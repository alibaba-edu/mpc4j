package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory.ZpType;
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
 * Zp功能测试。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
@RunWith(Parameterized.class)
public class ZpTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        ZpType[] types = new ZpType[]{ZpType.JDK};
        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 61, 62, 63, 64, 65, 127, 128, 129};
        for (ZpType type : types) {
            // add each l
            for (int l : ls) {
                configurations.add(new Object[]{type.name() + ", l = " + l, type, l});
            }
        }

        return configurations;
    }

    /**
     * Zp运算类型
     */
    private final ZpType type;
    /**
     * Zp有限域
     */
    private final Zp zp;

    public ZpTest(String name, ZpType type, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        zp = ZpFactory.createInstance(EnvType.STANDARD, type, l);
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, zp.getZpType());
    }

    @Test
    public void testElementBitLength() {
        int elementBitLength = zp.getElementBitLength();
        int l = zp.getL();
        Assert.assertEquals(elementBitLength, l + 1);
    }

    @Test
    public void testElementByteLength() {
        int elementByteLength = zp.getElementByteLength();
        int byteL = zp.getByteL();
        if (zp.getL() % Byte.SIZE == 0) {
            // 如果l刚好可以被Byte.SIZE整除，则质数字节长度会更大一点
            Assert.assertEquals(elementByteLength, byteL + 1);
        } else {
            Assert.assertEquals(elementByteLength, byteL);
        }
    }

    @Test
    public void testModulus() {
        // 0 mod p = 0
        Assert.assertEquals(BigInteger.ZERO, zp.module(BigInteger.ZERO));
        // 1 mod p = 1
        Assert.assertEquals(BigInteger.ONE, zp.module(BigInteger.ONE));
        // p + 0 mod p = 0
        Assert.assertEquals(BigInteger.ZERO, zp.module(zp.getPrime()));
        // p + 1 mod p = 1
        Assert.assertEquals(BigInteger.ONE, zp.module(zp.getPrime().add(BigInteger.ONE)));
        // -1 mod p = p - 1
        Assert.assertEquals(zp.getPrime().subtract(BigInteger.ONE), zp.module(BigInteger.ONE.negate()));
        // 1 - p mod p = 1
        Assert.assertEquals(BigInteger.ONE, zp.module(BigInteger.ONE.subtract(zp.getPrime())));
    }

    @Test
    public void testConstantAddNegSub() {
        BigInteger one = BigInteger.ONE;
        BigInteger two = BigInteger.valueOf(2);
        BigInteger four = BigInteger.valueOf(4);
        BigInteger prime = zp.getPrime();
        if (BigIntegerUtils.greater(prime, two)) {
            // 1 + 1 = 2
            Assert.assertEquals(two, zp.add(one, one));
            // -1 = p - 1
            Assert.assertEquals(prime.subtract(one), zp.neg(one));
            // 2 - 1 = 1
            Assert.assertEquals(one, zp.sub(two, one));
        }
        if (BigIntegerUtils.greater(prime, four)) {
            // 2 + 2 = 4
            Assert.assertEquals(four, zp.add(two, two));
            // -2 = p - 2
            Assert.assertEquals(prime.subtract(two), zp.neg(two));
            // 4 - 2 = 2
            Assert.assertEquals(two, zp.sub(four, two));
        }
    }

    @Test
    public void testConstantMulInvDiv() {
        BigInteger two = BigInteger.valueOf(2);
        BigInteger four = BigInteger.valueOf(4);
        BigInteger prime = zp.getPrime();
        if (BigIntegerUtils.greater(prime, four)) {
            // 2 * 2 = 4
            Assert.assertEquals(four, zp.mul(two, two));
            // 4 * 2^{-1} = 2
            Assert.assertEquals(two, zp.mul(four, zp.inv(two)));
            // 4 / 2 = 2
            Assert.assertEquals(two, zp.div(four, two));
        }
    }
}
