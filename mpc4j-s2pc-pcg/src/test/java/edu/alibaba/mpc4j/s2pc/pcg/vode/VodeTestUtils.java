package edu.alibaba.mpc4j.s2pc.pcg.vode;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeSenderOutput;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.junit.Assert;

import java.util.stream.IntStream;

/**
 * VODE (Vector Oblivious Direct Evaluation) test utilities.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public class VodeTestUtils {
    /**
     * private constructor.
     */
    private VodeTestUtils() {
        // empty
    }

    /**
     * Verifies output.
     *
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(Dgf2k field, int num,
                                    Gf2kVodeSenderOutput senderOutput, Gf2kVodeReceiverOutput receiverOutput) {
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
    public static void assertOutput(Dgf2k field, int num,
                                    Gf2kSspVodeSenderOutput senderOutput, Gf2kSspVodeReceiverOutput receiverOutput) {
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
    public static void assertOutput(Dgf2k field, int num,
                                    Gf2kMspVodeSenderOutput senderOutput, Gf2kMspVodeReceiverOutput receiverOutput) {
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
}
