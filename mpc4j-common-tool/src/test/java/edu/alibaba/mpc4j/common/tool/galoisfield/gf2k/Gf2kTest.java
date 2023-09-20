package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * GF(2^128) test.
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
@RunWith(Parameterized.class)
public class Gf2kTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (Gf2kType type : Gf2kType.values()) {
            configurations.add(new Object[]{type.name(), type,});
        }

        return configurations;
    }

    /**
     * GF(2^128) type
     */
    private final Gf2kType type;
    /**
     * GF(2^128) instance
     */
    private final Gf2k gf2k;

    public Gf2kTest(String name, Gf2kType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        gf2k = Gf2kFactory.createInstance(EnvType.STANDARD, type);
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, gf2k.getGf2kType());
    }

    @Test
    public void testElementBitLength() {
        int elementBitLength = gf2k.getElementBitLength();
        int l = gf2k.getL();
        Assert.assertEquals(elementBitLength, l);
    }

    @Test
    public void testElementByteLength() {
        int elementByteLength = gf2k.getElementByteLength();
        int byteL = gf2k.getByteL();
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
        p = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};
        q = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};
        expect = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04};
        // mul
        actual = gf2k.mul(p, q);
        Assert.assertArrayEquals(expect, actual);
        copyP = BytesUtils.clone(p);
        // muli
        gf2k.muli(copyP, q);
        Assert.assertArrayEquals(expect, copyP);
        copyP = BytesUtils.clone(p);
        // self muli
        gf2k.muli(copyP, copyP);
        Assert.assertArrayEquals(expect, copyP);

        // x^2 / x = x
        p = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04};
        q = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};
        expect = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};
        // div
        actual = gf2k.div(p, q);
        Assert.assertArrayEquals(expect, actual);
        // divi
        copyP = BytesUtils.clone(p);
        gf2k.divi(copyP, q);
        Assert.assertArrayEquals(expect, copyP);

        // x^2 * x^2 = x^4
        p = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04};
        q = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04};
        expect = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10};
        // mul
        actual = gf2k.mul(p, q);
        Assert.assertArrayEquals(expect, actual);
        // muli
        copyP = BytesUtils.clone(p);
        gf2k.muli(copyP, q);
        Assert.assertArrayEquals(expect, copyP);
        copyP = BytesUtils.clone(p);
        // self muli
        gf2k.muli(copyP, copyP);
        Assert.assertArrayEquals(expect, copyP);
        // x^4 / x^2 = x^2
        p = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10};
        q = new byte[]{
            (byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04
        };
        expect = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04};
        // div
        actual = gf2k.div(p, q);
        Assert.assertArrayEquals(expect, actual);
        // divi
        copyP = BytesUtils.clone(p);
        gf2k.divi(copyP, q);
        Assert.assertArrayEquals(expect, copyP);
    }
}
