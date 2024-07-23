package edu.alibaba.mpc4j.s2pc.pcg.vole;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.Gf2eVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e.Gf2eVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.Gf2kMspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.Gf2kMspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleSenderOutput;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.junit.Assert;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * VOLE test utilities.
 *
 * @author Weiran Liu
 * @date 2024/5/28
 */
public class VoleTestUtils {
    /**
     * private constructor.
     */
    private VoleTestUtils() {
        // empty
    }

    /**
     * Verifies output.
     *
     * @param field          field.
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(Gf2e field, int num,
                                    Gf2eVoleSenderOutput senderOutput, Gf2eVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(field, senderOutput.getField());
        Assert.assertEquals(field, receiverOutput.getField());
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getX().length);
            Assert.assertEquals(0, senderOutput.getT().length);
            Assert.assertEquals(0, receiverOutput.getQ().length);
        } else {
            IntStream.range(0, num).forEach(index -> {
                byte[] actualTi = field.mul(senderOutput.getX(index), receiverOutput.getDelta());
                field.addi(actualTi, receiverOutput.getQ(index));
                Assert.assertArrayEquals(senderOutput.getT(index), actualTi);
            });
        }
    }

    /**
     * Verifies output.
     *
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(Sgf2k field, int num,
                                    Gf2kVoleSenderOutput senderOutput, Gf2kVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(field, senderOutput.getField());
        Assert.assertEquals(senderOutput.getField(), receiverOutput.getField());
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getX().length);
            Assert.assertEquals(0, senderOutput.getT().length);
            Assert.assertEquals(0, receiverOutput.getQ().length);
        } else {
            IntStream.range(0, num).forEach(index -> {
                byte[] ti = field.mixMul(senderOutput.getX(index), receiverOutput.getDelta());
                field.addi(ti, receiverOutput.getQ(index));
                Assert.assertArrayEquals(senderOutput.getT(index), ti);
            });
        }
    }

    /**
     * Verifies output.
     *
     * @param field          field.
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(Sgf2k field, int num,
                                    Gf2kSspVoleSenderOutput senderOutput, Gf2kSspVoleReceiverOutput receiverOutput) {
        Assert.assertTrue(num > 0);
        Assert.assertEquals(field, senderOutput.getField());
        Assert.assertEquals(senderOutput.getField(), receiverOutput.getField());
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        Gf2e subfield = field.getSubfield();
        IntStream.range(0, num).forEach(index -> {
            byte[] t = senderOutput.getT(index);
            byte[] q = receiverOutput.getQ(index);
            byte[] x = senderOutput.getX(index);
            byte[] delta = receiverOutput.getDelta();
            byte[] v = field.mixMul(x, delta);
            field.addi(v, q);
            Assert.assertArrayEquals(t, v);
            if (index == senderOutput.getAlpha()) {
                // x is non-zero
                Assert.assertFalse(subfield.isZero(x));
            } else {
                // x is zero
                Assert.assertTrue(subfield.isZero(x));
            }
        });
    }

    /**
     * Verifies output.
     *
     * @param field          field.
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(Sgf2k field, int num,
                                    Gf2kMspVoleSenderOutput senderOutput, Gf2kMspVoleReceiverOutput receiverOutput) {
        Assert.assertTrue(num > 0);
        Assert.assertEquals(field, senderOutput.getField());
        Assert.assertEquals(senderOutput.getField(), receiverOutput.getField());
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        TIntSet alphaSet = new TIntHashSet(senderOutput.getAlphaArray());
        Gf2e subfield = field.getSubfield();
        IntStream.range(0, num).forEach(index -> {
            // w = v + Δ · u
            byte[] w = senderOutput.getT(index);
            byte[] v = receiverOutput.getQ(index);
            byte[] u = senderOutput.getX(index);
            byte[] delta = receiverOutput.getDelta();
            byte[] vPrime = field.mixMul(u, delta);
            field.addi(vPrime, v);
            Assert.assertArrayEquals(w, vPrime);
            if (alphaSet.contains(index)) {
                // u is non-zero
                Assert.assertFalse(subfield.isZero(u));
            } else {
                // u is zero
                Assert.assertTrue(subfield.isZero(u));
            }
        });
    }

    /**
     * Asserts output.
     *
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(int num, ZpVoleSenderOutput senderOutput, ZpVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getZp(), receiverOutput.getZp());
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getX().length);
            Assert.assertEquals(0, senderOutput.getT().length);
            Assert.assertEquals(0, receiverOutput.getQ().length);
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            Zp zp = senderOutput.getZp();
            IntStream.range(0, num).forEach(i -> {
                BigInteger actualT = zp.mul(senderOutput.getX(i), receiverOutput.getDelta());
                actualT = zp.add(actualT, receiverOutput.getQ(i));
                BigInteger expectT = senderOutput.getT(i);
                Assert.assertEquals(expectT, actualT);
            });
        }
    }

    /**
     * Asserts output.
     *
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(int num, Zp64VoleSenderOutput senderOutput, Zp64VoleReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getZp64(), receiverOutput.getZp64());
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getX().length);
            Assert.assertEquals(0, senderOutput.getT().length);
            Assert.assertEquals(0, receiverOutput.getQ().length);
        } else {
            Zp64 zp64 = senderOutput.getZp64();
            IntStream.range(0, num).forEach(i -> {
                long actualT = zp64.mul(senderOutput.getX(i), receiverOutput.getDelta());
                actualT = zp64.add(actualT, receiverOutput.getQ(i));
                Assert.assertEquals(senderOutput.getT(i), actualT);
            });
        }
    }
}
