package edu.alibaba.mpc4j.s2pc.aby.pcg.triple;

import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.structure.vector.Zp64Vector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import org.junit.Assert;

import java.math.BigInteger;

/**
 * Triple test utilities.
 *
 * @author Weiran Liu
 * @date 2024/5/25
 */
public class TripleTestUtils {
    /**
     * private constructor.
     */
    private TripleTestUtils() {
        // empty
    }

    /**
     * asserts triples.
     *
     * @param num            num.
     * @param senderTriple   sender triple.
     * @param receiverTriple receiver triple.
     */
    public static void assertOutput(int num, Z2Triple senderTriple, Z2Triple receiverTriple) {
        Assert.assertEquals(num, senderTriple.getNum());
        Assert.assertEquals(num, receiverTriple.getNum());
        if (num == 0) {
            // sender num
            Assert.assertEquals(0, senderTriple.getByteNum());
            // sender a
            Assert.assertArrayEquals(new byte[0], senderTriple.getA());
            Assert.assertEquals("", senderTriple.getStringA());
            // sender b
            Assert.assertArrayEquals(new byte[0], senderTriple.getB());
            Assert.assertEquals("", senderTriple.getStringB());
            // sender c
            Assert.assertArrayEquals(new byte[0], senderTriple.getC());
            Assert.assertEquals("", senderTriple.getStringC());
            // receiver num
            Assert.assertEquals(0, receiverTriple.getByteNum());
            // receiver a
            Assert.assertArrayEquals(new byte[0], receiverTriple.getA());
            Assert.assertEquals("", receiverTriple.getStringA());
            // receiver b
            Assert.assertArrayEquals(new byte[0], receiverTriple.getB());
            Assert.assertEquals("", receiverTriple.getStringB());
            // receiver c
            Assert.assertArrayEquals(new byte[0], receiverTriple.getC());
            Assert.assertEquals("", receiverTriple.getStringC());
        } else {
            BitVector a = senderTriple.getVectorA().xor(receiverTriple.getVectorA());
            BitVector b = senderTriple.getVectorB().xor(receiverTriple.getVectorB());
            BitVector c = senderTriple.getVectorC().xor(receiverTriple.getVectorC());
            Assert.assertEquals(c, a.and(b));
        }
    }

    /**
     * asserts triples.
     *
     * @param zl             Zl instance.
     * @param num            num.
     * @param senderTriple   sender triple.
     * @param receiverTriple receiver triple.
     */
    public static void assertOutput(Zl zl, int num, ZlTriple senderTriple, ZlTriple receiverTriple) {
        Assert.assertEquals(zl, senderTriple.getZl());
        Assert.assertEquals(zl, receiverTriple.getZl());
        Assert.assertEquals(num, senderTriple.getNum());
        Assert.assertEquals(num, receiverTriple.getNum());
        if (num == 0) {
            Assert.assertArrayEquals(new BigInteger[0], senderTriple.getA());
            Assert.assertArrayEquals(new BigInteger[0], senderTriple.getB());
            Assert.assertArrayEquals(new BigInteger[0], senderTriple.getC());
            Assert.assertArrayEquals(new BigInteger[0], receiverTriple.getA());
            Assert.assertArrayEquals(new BigInteger[0], receiverTriple.getB());
            Assert.assertArrayEquals(new BigInteger[0], receiverTriple.getC());
        } else {
            ZlVector a = senderTriple.getVectorA().add(receiverTriple.getVectorA());
            ZlVector b = senderTriple.getVectorB().add(receiverTriple.getVectorB());
            ZlVector c = senderTriple.getVectorC().add(receiverTriple.getVectorC());
            Assert.assertEquals(a.mul(b), c);
        }
    }

    /**
     * asserts triples.
     *
     * @param num            num.
     * @param senderTriple   sender triple.
     * @param receiverTriple receiver triple.
     */
    public static void assertOutput(Zp64 zp64, int num, Zp64Triple senderTriple, Zp64Triple receiverTriple) {
        Assert.assertEquals(zp64, senderTriple.getZp64());
        Assert.assertEquals(zp64, receiverTriple.getZp64());
        Assert.assertEquals(num, senderTriple.getNum());
        Assert.assertEquals(num, receiverTriple.getNum());
        if (num == 0) {
            Assert.assertArrayEquals(new long[0], senderTriple.getA());
            Assert.assertArrayEquals(new long[0], senderTriple.getB());
            Assert.assertArrayEquals(new long[0], senderTriple.getC());
            Assert.assertArrayEquals(new long[0], receiverTriple.getA());
            Assert.assertArrayEquals(new long[0], receiverTriple.getB());
            Assert.assertArrayEquals(new long[0], receiverTriple.getC());
        } else {
            Zp64Vector a = senderTriple.getVectorA().add(receiverTriple.getVectorA());
            Zp64Vector b = senderTriple.getVectorB().add(receiverTriple.getVectorB());
            Zp64Vector c = senderTriple.getVectorC().add(receiverTriple.getVectorC());
            Assert.assertEquals(a.mul(b), c);
        }
    }

    /**
     * asserts triples.
     *
     * @param num            num.
     * @param senderTriple   sender triple.
     * @param receiverTriple receiver triple.
     */
    public static void assertOutput(Zl64 zl64, int num, Zl64Triple senderTriple, Zl64Triple receiverTriple) {
        Assert.assertEquals(zl64, senderTriple.getZl64());
        Assert.assertEquals(zl64, receiverTriple.getZl64());
        Assert.assertEquals(num, senderTriple.getNum());
        Assert.assertEquals(num, receiverTriple.getNum());
        if (num == 0) {
            Assert.assertArrayEquals(new long[0], senderTriple.getA());
            Assert.assertArrayEquals(new long[0], senderTriple.getB());
            Assert.assertArrayEquals(new long[0], senderTriple.getC());
            Assert.assertArrayEquals(new long[0], receiverTriple.getA());
            Assert.assertArrayEquals(new long[0], receiverTriple.getB());
            Assert.assertArrayEquals(new long[0], receiverTriple.getC());
        } else {
            Zl64Vector a = senderTriple.getVectorA().add(receiverTriple.getVectorA());
            Zl64Vector b = senderTriple.getVectorB().add(receiverTriple.getVectorB());
            Zl64Vector c = senderTriple.getVectorC().add(receiverTriple.getVectorC());
            Assert.assertEquals(c, a.mul(b));
        }
    }
}
