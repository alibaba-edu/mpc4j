package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * GF(2^l)功能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
@RunWith(Parameterized.class)
public class Gf2eTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        Gf2eType[] gf2eTypes = new Gf2eType[]{Gf2eType.NTL, Gf2eType.RINGS};
        int[] ls = new int[]{1, 2, 3, 4, 39, 40, 41, 128, 256};
        for (Gf2eType type : gf2eTypes) {
            // add each l
            for (int l : ls) {
                configurations.add(new Object[]{
                    Gf2eType.class.getSimpleName() + " (" + type.name() + ", l = " + l + ")", type, l
                });
            }
        }

        return configurations;
    }

    /**
     * GF(2^l)运算类型
     */
    private final Gf2eType type;
    /**
     * 有限域字节长度
     */
    private final int byteL;
    /**
     * GF(2^l)运算
     */
    private final Gf2e gf2e;

    public Gf2eTest(String name, Gf2eType type, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        gf2e = Gf2eFactory.createInstance(EnvType.STANDARD, type, l);
        byteL = gf2e.getByteL();
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, gf2e.getGf2eType());
    }

    @Test
    public void testElementBitLength() {
        int elementBitLength = gf2e.getElementBitLength();
        int l = gf2e.getL();
        Assert.assertEquals(elementBitLength, l);
    }

    @Test
    public void testElementByteLength() {
        int elementByteLength = gf2e.getElementByteLength();
        int byteL = gf2e.getByteL();
        Assert.assertEquals(elementByteLength, byteL);
    }

    @Test
    public void testGf2eConstantMulDiv() {
        byte[] p;
        byte[] copyP;
        byte[] q;
        byte[] t;
        byte[] truth;
        if (gf2e.getL() > 2) {
            // x * x = x^2
            p = new byte[byteL];
            p[p.length - 1] = 0x02;
            q = new byte[byteL];
            q[p.length - 1] = 0x02;
            truth = new byte[byteL];
            truth[p.length - 1] = 0x04;
            // mul
            t = gf2e.mul(p, q);
            Assert.assertArrayEquals(truth, t);
            // muli
            copyP = BytesUtils.clone(p);
            gf2e.muli(copyP, q);
            Assert.assertArrayEquals(truth, copyP);
            // self muli
            copyP = BytesUtils.clone(p);
            gf2e.muli(copyP, q);
            Assert.assertArrayEquals(truth, copyP);

            // x^2 / x = x
            p = new byte[byteL];
            p[p.length - 1] = 0x04;
            q = new byte[byteL];
            q[p.length - 1] = 0x02;
            truth = new byte[byteL];
            truth[p.length - 1] = 0x02;
            // div
            t = gf2e.div(p, q);
            Assert.assertArrayEquals(truth, t);
            // divi
            copyP = BytesUtils.clone(p);
            gf2e.divi(copyP, q);
            Assert.assertArrayEquals(truth, copyP);
        }
        if (gf2e.getL() > 4) {
            // x^4 / x^2 = x^2
            p = new byte[byteL];
            p[p.length - 1] = 0x10;
            q = new byte[byteL];
            q[p.length - 1] = 0x04;
            truth = new byte[byteL];
            truth[p.length - 1] = 0x04;
            // div
            t = gf2e.div(p, q);
            Assert.assertArrayEquals(truth, t);
            // divi
            copyP = BytesUtils.clone(p);
            gf2e.divi(copyP, q);
            Assert.assertArrayEquals(truth, copyP);
        }
    }
}
