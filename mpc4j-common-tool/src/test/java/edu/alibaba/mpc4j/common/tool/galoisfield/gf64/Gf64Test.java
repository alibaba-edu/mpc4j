package edu.alibaba.mpc4j.common.tool.galoisfield.gf64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory.Gf64Type;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * GF(2^64) type.
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
@RunWith(Parameterized.class)
public class Gf64Test {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (Gf64Type type : Gf64Type.values()) {
            configurations.add(new Object[]{type.name(), type,});
        }

        return configurations;
    }

    /**
     * GF(2^64) type
     */
    private final Gf64Type type;
    /**
     * GF(2^64) instance
     */
    private final Gf64 gf64;

    public Gf64Test(String name, Gf64Type type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        gf64 = Gf64Factory.createInstance(EnvType.STANDARD, type);
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, gf64.getGf64Type());
    }

    @Test
    public void testElementBitLength() {
        int elementBitLength = gf64.getElementBitLength();
        int l = gf64.getL();
        Assert.assertEquals(elementBitLength, l);
    }

    @Test
    public void testElementByteLength() {
        int elementByteLength = gf64.getElementByteLength();
        int byteL = gf64.getByteL();
        Assert.assertEquals(elementByteLength, byteL);
    }

    @Test
    public void testConstantMulDiv() {
        byte[] p;
        byte[] copyP;
        byte[] q;
        byte[] actual;
        byte[] expect;
        // x * x = x^2
        p = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};
        q = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};
        expect = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04};
        // mul
        actual = gf64.mul(p, q);
        Assert.assertArrayEquals(expect, actual);
        copyP = BytesUtils.clone(p);
        // muli
        gf64.muli(copyP, q);
        Assert.assertArrayEquals(expect, copyP);
        copyP = BytesUtils.clone(p);
        // self muli
        gf64.muli(copyP, copyP);
        Assert.assertArrayEquals(expect, copyP);

        // x^2 * x^2 = x^4
        p = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04};
        q = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04};
        expect = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10};
        // mul
        actual = gf64.mul(p, q);
        Assert.assertArrayEquals(expect, actual);
        // muli
        copyP = BytesUtils.clone(p);
        gf64.muli(copyP, q);
        Assert.assertArrayEquals(expect, copyP);
        copyP = BytesUtils.clone(p);
        // self muli
        gf64.muli(copyP, copyP);
        Assert.assertArrayEquals(expect, copyP);
    }
}
