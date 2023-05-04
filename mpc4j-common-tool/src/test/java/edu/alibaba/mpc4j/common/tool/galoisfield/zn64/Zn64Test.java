package edu.alibaba.mpc4j.common.tool.galoisfield.zn64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zn64.Zn64Factory.Zn64Type;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Zn64 tests.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
@RunWith(Parameterized.class)
public class Zn64Test {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        Zn64Type[] types = new Zn64Type[]{Zn64Type.RINGS};
        long[] ns = new long[]{2, 3, 4, 7, 8, 247, 350, 511, 512, 513, 701, 833, 991, 1023, 1024, 1025};
        for (Zn64Type type : types) {
            // add each n
            for (long n : ns) {
                configurations.add(new Object[]{type.name() + ", n = " + n, type, n});
            }
        }

        return configurations;
    }

    /**
     * the type
     */
    private final Zn64Type type;
    /**
     * the Zn64 instance
     */
    private final Zn64 zn64;

    public Zn64Test(String name, Zn64Type type, long n) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        zn64 = Zn64Factory.createInstance(EnvType.STANDARD, type, n);
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, zn64.getZn64Type());
    }

    @Test
    public void testElementBitLength() {
        int elementBitLength = zn64.getElementBitLength();
        int l = zn64.getL();
        Assert.assertEquals(elementBitLength, l + 1);
    }

    @Test
    public void testElementByteLength() {
        int elementByteLength = zn64.getElementByteLength();
        int byteL = zn64.getByteL();
        int l = zn64.getL();
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
        Assert.assertEquals(0L, zn64.module(0L));
        // 1 mod n = 1
        Assert.assertEquals(1L, zn64.module(1L));
        // n + 0 mod n = 0
        Assert.assertEquals(0L, zn64.module(zn64.getN()));
        // n + 1 mod n = 1
        Assert.assertEquals(1L, zn64.module(zn64.getN() + 1L));
        // -1 mod n = n - 1
        Assert.assertEquals(zn64.getN() - 1L, zn64.module(-1L));
        // 1 - n mod n = 1
        Assert.assertEquals(1L, zn64.module(1 - zn64.getN()));
    }

    @Test
    public void testConstantAddNegSub() {
        long n = zn64.getN();
        if (n > 2L) {
            // 1 + 1 = 2
            Assert.assertEquals(2L, zn64.add(1L, 1L));
            // -1 = n - 1
            Assert.assertEquals(n - 1L, zn64.neg(1L));
            // 2 - 1 = 1
            Assert.assertEquals(1L, zn64.sub(2L, 1L));
        }
        if (n > 4L) {
            // 2 + 2 = 4
            Assert.assertEquals(4, zn64.add(2L, 2L));
            // -2 = n - 2
            Assert.assertEquals(n - 2L, zn64.neg(2L));
            // 4 - 2 = 2
            Assert.assertEquals(2L, zn64.sub(4L, 2L));
        }
    }

    @Test
    public void testConstantMul() {
        long n = zn64.getN();
        if (n > 4) {
            // 2 * 2 = 4
            Assert.assertEquals(4L, zn64.mul(2L, 2L));
        }
    }
}
