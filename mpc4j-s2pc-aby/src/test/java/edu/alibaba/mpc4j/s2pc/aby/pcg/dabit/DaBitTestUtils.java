package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit;

import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import org.junit.Assert;

import java.math.BigInteger;

/**
 * daBit test utilities.
 *
 * @author Weiran Liu
 * @date 2024/7/1
 */
public class DaBitTestUtils {
    /**
     * private constructor.
     */
    private DaBitTestUtils() {
        // empty
    }

    /**
     * asserts output.
     *
     * @param zl            Zl.
     * @param num           num.
     * @param senderTuple   sender tuple.
     * @param receiverTuple receiver tuple.
     */
    public static void assertOutput(Zl zl, int num, ZlDaBitTuple senderTuple, ZlDaBitTuple receiverTuple) {
        Assert.assertEquals(zl, senderTuple.getZl());
        Assert.assertEquals(zl, receiverTuple.getZl());
        Assert.assertEquals(num, senderTuple.getNum());
        Assert.assertEquals(num, receiverTuple.getNum());
        Assert.assertEquals(senderTuple.getZl(), receiverTuple.getZl());
        if (num == 0) {
            Assert.assertArrayEquals(new byte[0], senderTuple.getSquareZ2Vector().getBitVector().getBytes());
            Assert.assertArrayEquals(new BigInteger[0], senderTuple.getSquareZlVector().getZlVector().getElements());
            Assert.assertArrayEquals(new byte[0], receiverTuple.getSquareZ2Vector().getBitVector().getBytes());
            Assert.assertArrayEquals(new BigInteger[0], receiverTuple.getSquareZlVector().getZlVector().getElements());
        } else {
            SquareZ2Vector senderSquareZ2Vector = senderTuple.getSquareZ2Vector();
            SquareZ2Vector receiverSquareZ2Vector = receiverTuple.getSquareZ2Vector();
            BitVector bitVector = senderSquareZ2Vector.getBitVector().xor(receiverSquareZ2Vector.getBitVector());
            SquareZlVector senderSquareZlVector = senderTuple.getSquareZlVector();
            SquareZlVector receiverSquareZlVector = receiverTuple.getSquareZlVector();
            ZlVector zlVector = senderSquareZlVector.getZlVector().add(receiverSquareZlVector.getZlVector());
            for (int i = 0; i < num; i++) {
                if (bitVector.get(i)) {
                    Assert.assertTrue(zl.isOne(zlVector.getElement(i)));
                } else {
                    Assert.assertTrue(zl.isZero(zlVector.getElement(i)));
                }
            }
        }
    }
}
