package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory.Zp64Type;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Zp64功能测试。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
@RunWith(Parameterized.class)
public class Zp64Test {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        Zp64Type[] zp64Types = new Zp64Type[]{Zp64Type.RINGS};
        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 61, 62};
        for (Zp64Type type : zp64Types) {
            // add each l
            for (int l : ls) {
                configurations.add(new Object[]{type.name() + " (l = " + l + ")", type, l});
            }
        }

        return configurations;
    }

    /**
     * Zp64运算类型
     */
    private final Zp64Type zp64Type;
    /**
     * Zp64有限域
     */
    private final Zp64 zp64;

    public Zp64Test(String name, Zp64Type zp64Type, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.zp64Type = zp64Type;
        zp64 = Zp64Factory.createInstance(EnvType.STANDARD, zp64Type, l);
    }

    @Test
    public void testType() {
        Assert.assertEquals(zp64Type, zp64.getZp64Type());
    }

    @Test
    public void testElementBitLength() {
        int elementBitLength = zp64.getElementBitLength();
        int l = zp64.getL();
        Assert.assertEquals(elementBitLength, l + 1);
    }

    @Test
    public void testElementByteLength() {
        int elementByteLength = zp64.getElementByteLength();
        int byteL = zp64.getByteL();
        if (zp64.getL() % Byte.SIZE == 0) {
            // 如果l刚好可以被Byte.SIZE整除，则质数字节长度会更大一点
            Assert.assertEquals(elementByteLength, byteL + 1);
        } else {
            Assert.assertEquals(elementByteLength, byteL);
        }
    }

    @Test
    public void testModulus() {
        // 0 mod p = 0
        Assert.assertEquals(0L, zp64.module(0L));
        // 1 mod p = 1
        Assert.assertEquals(1L, zp64.module(1L));
        // p + 0 mod p = 0
        Assert.assertEquals(0L, zp64.module(zp64.getPrime()));
        // p + 1 mod p = 1
        Assert.assertEquals(1L, zp64.module(zp64.getPrime() + 1));
        // -1 mod p = p - 1
        Assert.assertEquals(zp64.getPrime() - 1, zp64.module(-1L));
        // 1 - p mod p = 1
        Assert.assertEquals(1L, zp64.module(1L - zp64.getPrime()));
    }

    @Test
    public void testConstantAddNegSub() {
        long prime = zp64.getPrime();
        if (prime > 2) {
            // 1 + 1 = 2
            Assert.assertEquals(2, zp64.add(1, 1));
            // -1 = prime - 1
            Assert.assertEquals(prime - 1, zp64.neg(1));
            // 2 - 1 = 1
            Assert.assertEquals(1, zp64.sub(2, 1));
        }
        if (prime > 4) {
            // 2 + 2 = 4
            Assert.assertEquals(4, zp64.add(2, 2));
            // -2 = prime - 2
            Assert.assertEquals(prime - 2, zp64.neg(2));
            // 4 - 2 = 2
            Assert.assertEquals(2, zp64.sub(4, 2));
        }
    }

    @Test
    public void testConstantMulInvDiv() {
        long prime = zp64.getPrime();
        if (prime > 4) {
            // 2 * 2 = 4
            Assert.assertEquals(4, zp64.mul(2, 2));
            // 4 * 2^{-1} = 2
            Assert.assertEquals(2, zp64.mul(4, zp64.inv(2)));
            // 4 / 2 = 2
            Assert.assertEquals(2, zp64.div(4, 2));
        }
    }
}
