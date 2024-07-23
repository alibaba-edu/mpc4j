package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit;

import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zl daBit vector test.
 *
 * @author Weiran Liu
 * @date 2024/7/1
 */
public class ZlDaBitTupleTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 32;
    /**
     * Zl
     */
    private final Zl zl;
    /**
     * large Zl
     */
    private final Zl largeZl;
    /**
     * Zl array
     */
    private final Zl[] zlArray;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public ZlDaBitTupleTest() {
        zl = ZlFactory.createInstance(EnvType.STANDARD, 32);
        largeZl = ZlFactory.createInstance(EnvType.STANDARD, 40);
        zlArray = IntStream.range(1, 128)
            .mapToObj(l -> ZlFactory.createInstance(EnvType.STANDARD, l))
            .toArray(Zl[]::new);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            SquareZ2Vector squareZ2Vector = SquareZ2Vector.createEmpty(true);
            SquareZlVector squareZlVector = SquareZlVector.createEmpty(zl, true);
            ZlDaBitTuple.create(squareZlVector, squareZ2Vector);
        });
        int num = 12;
        // create tuple with plain square vector
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BitVector bitVector = BitVectorFactory.createRandom(num, secureRandom);
            SquareZ2Vector squareZ2Vector = SquareZ2Vector.create(bitVector, true);
            ZlVector zlVector = ZlVector.createRandom(zl, num - 1, secureRandom);
            SquareZlVector squareZlVector = SquareZlVector.create(zlVector, true);
            ZlDaBitTuple.create(squareZlVector, squareZ2Vector);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BitVector bitVector = BitVectorFactory.createRandom(num, secureRandom);
            SquareZ2Vector squareZ2Vector = SquareZ2Vector.create(bitVector, false);
            ZlVector zlVector = ZlVector.createRandom(zl, num - 1, secureRandom);
            SquareZlVector squareZlVector = SquareZlVector.create(zlVector, true);
            ZlDaBitTuple.create(squareZlVector, squareZ2Vector);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BitVector bitVector = BitVectorFactory.createRandom(num, secureRandom);
            SquareZ2Vector squareZ2Vector = SquareZ2Vector.create(bitVector, true);
            ZlVector zlVector = ZlVector.createRandom(zl, num - 1, secureRandom);
            SquareZlVector squareZlVector = SquareZlVector.create(zlVector, false);
            ZlDaBitTuple.create(squareZlVector, squareZ2Vector);
        });
        // create tuple with mismatch num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BitVector bitVector = BitVectorFactory.createRandom(num, secureRandom);
            SquareZ2Vector squareZ2Vector = SquareZ2Vector.create(bitVector, false);
            ZlVector zlVector = ZlVector.createRandom(zl, num - 1, secureRandom);
            SquareZlVector squareZlVector = SquareZlVector.create(zlVector, false);
            ZlDaBitTuple.create(squareZlVector, squareZ2Vector);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BitVector bitVector = BitVectorFactory.createRandom(num, secureRandom);
            SquareZ2Vector squareZ2Vector = SquareZ2Vector.create(bitVector, false);
            ZlVector zlVector = ZlVector.createRandom(zl, num + 1, secureRandom);
            SquareZlVector squareZlVector = SquareZlVector.create(zlVector, false);
            ZlDaBitTuple.create(squareZlVector, squareZ2Vector);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BitVector bitVector = BitVectorFactory.createRandom(num - 1, secureRandom);
            SquareZ2Vector squareZ2Vector = SquareZ2Vector.create(bitVector, false);
            ZlVector zlVector = ZlVector.createRandom(zl, num, secureRandom);
            SquareZlVector squareZlVector = SquareZlVector.create(zlVector, false);
            ZlDaBitTuple.create(squareZlVector, squareZ2Vector);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BitVector bitVector = BitVectorFactory.createRandom(num + 1, secureRandom);
            SquareZ2Vector squareZ2Vector = SquareZ2Vector.create(bitVector, false);
            ZlVector zlVector = ZlVector.createRandom(zl, num, secureRandom);
            SquareZlVector squareZlVector = SquareZlVector.create(zlVector, false);
            ZlDaBitTuple.create(squareZlVector, squareZ2Vector);
        });
    }

    @Test
    public void testIllegalUpdate() {
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlDaBitTuple vector = ZlDaBitTuple.createRandom(zl, 4, secureRandom);
            vector.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlDaBitTuple vector = ZlDaBitTuple.createRandom(zl, 4, secureRandom);
            vector.split(5);
        });
        // reduce with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlDaBitTuple vector = ZlDaBitTuple.createRandom(zl, 4, secureRandom);
            vector.reduce(0);
        });
        // reduce with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlDaBitTuple vector = ZlDaBitTuple.createRandom(zl, 4, secureRandom);
            vector.reduce(5);
        });
        // merge with different l
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlDaBitTuple vector = ZlDaBitTuple.createRandom(zl, 4, secureRandom);
            ZlDaBitTuple mergeVector = ZlDaBitTuple.createRandom(largeZl, 4, secureRandom);
            vector.merge(mergeVector);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        ZlDaBitTuple senderTuple = ZlDaBitTuple.createRandom(zl, num, secureRandom);
        ZlDaBitTuple receiverTuple = ZlDaBitTuple.createRandom(senderTuple, secureRandom);
        DaBitTestUtils.assertOutput(zl, num, senderTuple, receiverTuple);
    }

    @Test
    public void testReduce() {
        for (Zl zl : zlArray) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testReduce(zl, num);
            }
        }
    }

    private void testReduce(Zl zl, int num) {
        // reduce 1
        ZlDaBitTuple tuple1 = ZlDaBitTuple.createRandom(zl, num, secureRandom);
        tuple1.reduce(1);
        DaBitTestUtils.assertOutput(zl, 1, tuple1, ZlDaBitTuple.createRandom(tuple1, secureRandom));
        // reduce all
        ZlDaBitTuple tupleAll = ZlDaBitTuple.createRandom(zl, num, secureRandom);
        tupleAll.reduce(num);
        DaBitTestUtils.assertOutput(zl, num, tupleAll, ZlDaBitTuple.createRandom(tupleAll, secureRandom));
        if (num > 1) {
            // reduce num - 1
            ZlDaBitTuple tupleNum = ZlDaBitTuple.createRandom(zl, num, secureRandom);
            tupleNum.reduce(num - 1);
            DaBitTestUtils.assertOutput(zl, num - 1, tupleNum, ZlDaBitTuple.createRandom(tupleNum, secureRandom));
            // reduce half
            ZlDaBitTuple tupleHalf = ZlDaBitTuple.createRandom(zl, num, secureRandom);
            tupleHalf.reduce(num / 2);
            DaBitTestUtils.assertOutput(zl, num / 2, tupleHalf, ZlDaBitTuple.createRandom(tupleHalf, secureRandom));
        }
    }

    @Test
    public void testMerge() {
        for (Zl zl : zlArray) {
            for (int num1 = 0; num1 < MAX_NUM; num1++) {
                for (int num2 = 0; num2 < MAX_NUM; num2++) {
                    testMerge(zl, num1, num2);
                }
            }
        }
    }

    private void testMerge(Zl zl, int num1, int num2) {
        ZlDaBitTuple tuple = ZlDaBitTuple.createRandom(zl, num1, secureRandom);
        ZlDaBitTuple mergeTuple = ZlDaBitTuple.createRandom(zl, num2, secureRandom);
        tuple.merge(mergeTuple);
        DaBitTestUtils.assertOutput(zl, num1 + num2, tuple, ZlDaBitTuple.createRandom(tuple, secureRandom));
    }

    @Test
    public void testSplit() {
        for (Zl zl : zlArray) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testSplit(zl, num);
            }
        }
    }

    private void testSplit(Zl zl, int num) {
        // split 1
        ZlDaBitTuple tuple1 = ZlDaBitTuple.createRandom(zl, num, secureRandom);
        ZlDaBitTuple splitTuple1 = tuple1.split(1);
        DaBitTestUtils.assertOutput(zl, num - 1, tuple1, ZlDaBitTuple.createRandom(tuple1, secureRandom));
        DaBitTestUtils.assertOutput(zl, 1, splitTuple1, ZlDaBitTuple.createRandom(splitTuple1, secureRandom));
        // split all
        ZlDaBitTuple tupleAll = ZlDaBitTuple.createRandom(zl, num, secureRandom);
        ZlDaBitTuple splitTupleAll = tupleAll.split(num);
        DaBitTestUtils.assertOutput(zl, 0, tupleAll, ZlDaBitTuple.createRandom(tupleAll, secureRandom));
        DaBitTestUtils.assertOutput(zl, num, splitTupleAll, ZlDaBitTuple.createRandom(splitTupleAll, secureRandom));
        if (num > 1) {
            // split num - 1
            ZlDaBitTuple tupleNum = ZlDaBitTuple.createRandom(zl, num, secureRandom);
            ZlDaBitTuple splitTupleNum = tupleNum.split(num - 1);
            DaBitTestUtils.assertOutput(zl, 1, tupleNum, ZlDaBitTuple.createRandom(tupleNum, secureRandom));
            DaBitTestUtils.assertOutput(zl, num - 1, splitTupleNum, ZlDaBitTuple.createRandom(splitTupleNum, secureRandom));
            // split half
            ZlDaBitTuple tupleHalf = ZlDaBitTuple.createRandom(zl, num, secureRandom);
            ZlDaBitTuple splitTupleHalf = tupleHalf.split(num / 2);
            DaBitTestUtils.assertOutput(zl, num - num / 2, tupleHalf, ZlDaBitTuple.createRandom(tupleHalf, secureRandom));
            DaBitTestUtils.assertOutput(zl, num / 2, splitTupleHalf, ZlDaBitTuple.createRandom(splitTupleHalf, secureRandom));
        }
    }

    @Test
    public void testSplitMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplitMerge(num);
        }
    }

    private void testSplitMerge(int num) {
        // split and merge 1
        ZlDaBitTuple tuple1 = ZlDaBitTuple.createRandom(zl, num, secureRandom);
        ZlDaBitTuple copyTuple1 = tuple1.copy();
        ZlDaBitTuple splitTuple1 = tuple1.split(1);
        tuple1.merge(splitTuple1);
        Assert.assertEquals(copyTuple1, tuple1);
        // split and merge all
        ZlDaBitTuple tupleAll = ZlDaBitTuple.createRandom(zl, num, secureRandom);
        ZlDaBitTuple copyTupleAll = tupleAll.copy();
        ZlDaBitTuple splitTupleAll = tupleAll.split(num);
        tupleAll.merge(splitTupleAll);
        Assert.assertEquals(copyTupleAll, tupleAll);
        if (num > 1) {
            // split and merge num - 1
            ZlDaBitTuple tupleNum = ZlDaBitTuple.createRandom(zl, num, secureRandom);
            ZlDaBitTuple copyTupleNum = tupleNum.copy();
            ZlDaBitTuple splitTupleNum = tupleNum.split(num - 1);
            tupleNum.merge(splitTupleNum);
            Assert.assertEquals(copyTupleNum, tupleNum);
            // split half
            ZlDaBitTuple tupleHalf = ZlDaBitTuple.createRandom(zl, num, secureRandom);
            ZlDaBitTuple copyTupleHalf = tupleHalf.copy();
            ZlDaBitTuple splitTupleHalf = tupleHalf.split(num / 2);
            tupleHalf.merge(splitTupleHalf);
            Assert.assertEquals(copyTupleHalf, tupleHalf);
        }
    }
}
