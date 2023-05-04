package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * GF2E-VOLE output tests.
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
public class Gf2eVoleOutputTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 65;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * ls
     */
    private static final int[] LS = new int[]{1, 63, 64, 65, 127, 128, 129};
    /**
     * GF2E instances
     */
    private static final Gf2e[] GF2ES = Arrays.stream(LS)
        .mapToObj(l -> Gf2eFactory.createInstance(EnvType.STANDARD, l))
        .toArray(Gf2e[]::new);

    @Test
    public void testSenderIllegalInputs() {
        Gf2e defaultGf2e = Gf2eFactory.createInstance(EnvType.STANDARD, CommonConstants.BLOCK_BIT_LENGTH);
        int defaultByteL = defaultGf2e.getByteL();
        // create a sender output with length 0
        Assert.assertThrows(AssertionError.class, () ->
            Gf2eVoleSenderOutput.create(defaultGf2e, new byte[0][], new byte[0][])
        );
        // create a sender output with mismatched length
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][] x = IntStream.range(0, MIN_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput.create(defaultGf2e, x, t);
        });
        // create a sender output with small length x
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] xi = new byte[defaultByteL - 1];
                    SECURE_RANDOM.nextBytes(xi);
                    return xi;
                })
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput.create(defaultGf2e, x, t);
        });
        // create a sender output with large length x
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] xi = new byte[defaultByteL + 1];
                    SECURE_RANDOM.nextBytes(xi);
                    return xi;
                })
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput.create(defaultGf2e, x, t);
        });
        // create a sender output with small length t
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] xi = new byte[defaultByteL - 1];
                    SECURE_RANDOM.nextBytes(xi);
                    return xi;
                })
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput.create(defaultGf2e, x, t);
        });
        // create a sender output with large length t
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] xi = new byte[defaultByteL + 1];
                    SECURE_RANDOM.nextBytes(xi);
                    return xi;
                })
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput.create(defaultGf2e, x, t);
        });
        // merge two sender outputs with different l
        Gf2e otherGf2e = Gf2eFactory.createInstance(EnvType.STANDARD, CommonConstants.BLOCK_BIT_LENGTH / 2);
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][] x0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] t0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput senderOutput0 = Gf2eVoleSenderOutput.create(defaultGf2e, x0, t0);
            byte[][] x1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> otherGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] t1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> otherGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput senderOutput1 = Gf2eVoleSenderOutput.create(otherGf2e, x1, t1);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testReceiverIllegalInputs() {
        Gf2e defaultGf2e = Gf2eFactory.createInstance(EnvType.STANDARD, CommonConstants.BLOCK_BIT_LENGTH);
        int defaultByteL = defaultGf2e.getByteL();
        // create a receiver output with length 0
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] delta = defaultGf2e.createRangeRandom(SECURE_RANDOM);
            Gf2eVoleReceiverOutput.create(defaultGf2e, delta, new byte[0][]);
        });
        // create a receiver output with small length Δ
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] delta = new byte[defaultByteL - 1];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleReceiverOutput.create(defaultGf2e, delta, q);
        });
        // create a receiver output with large length Δ
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] delta = new byte[defaultByteL + 1];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleReceiverOutput.create(defaultGf2e, delta, q);
        });
        // create a receiver output with small length q
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] delta = defaultGf2e.createRangeRandom(SECURE_RANDOM);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] qi = new byte[defaultByteL - 1];
                    SECURE_RANDOM.nextBytes(qi);
                    return qi;
                })
                .toArray(byte[][]::new);
            Gf2eVoleReceiverOutput.create(defaultGf2e, delta, q);
        });
        // create a receiver output large length q
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] delta = defaultGf2e.createRangeRandom(SECURE_RANDOM);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] qi = new byte[defaultByteL + 1];
                    SECURE_RANDOM.nextBytes(qi);
                    return qi;
                })
                .toArray(byte[][]::new);
            Gf2eVoleReceiverOutput.create(defaultGf2e, delta, q);
        });
        // merge two receiver output with different Δ
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] delta0 = defaultGf2e.createRangeRandom(SECURE_RANDOM);
            byte[] delta1 = defaultGf2e.createRangeRandom(SECURE_RANDOM);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleReceiverOutput receiverOutput0 = Gf2eVoleReceiverOutput.create(defaultGf2e, delta0, q);
            Gf2eVoleReceiverOutput receiverOutput1 = Gf2eVoleReceiverOutput.create(defaultGf2e, delta1, q);
            receiverOutput0.merge(receiverOutput1);
        });
        // merge two receiver outputs with different l
        Gf2e otherGf2e = Gf2eFactory.createInstance(EnvType.STANDARD, CommonConstants.BLOCK_BIT_LENGTH / 2);
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] delta0 = defaultGf2e.createRangeRandom(SECURE_RANDOM);
            byte[] delta1 = otherGf2e.createRangeRandom(SECURE_RANDOM);
            byte[][] q0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> defaultGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleReceiverOutput receiverOutput0 = Gf2eVoleReceiverOutput.create(defaultGf2e, delta0, q0);
            byte[][] q1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> otherGf2e.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2eVoleReceiverOutput receiverOutput1 = Gf2eVoleReceiverOutput.create(otherGf2e, delta1, q1);
            receiverOutput0.merge(receiverOutput1);
        });
    }

    @Test
    public void testReduce() {
        for (Gf2e gf2e : GF2ES) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testReduce(gf2e, num);
            }
        }
    }

    private void testReduce(Gf2e gf2e, int num) {
        byte[] delta = gf2e.createRangeRandom(SECURE_RANDOM);
        // reduce to 1
        Gf2eVoleReceiverOutput receiverOutput1 = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
        Gf2eVoleSenderOutput senderOutput1 = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        Gf2eVoleTestUtils.assertOutput(gf2e, 1, senderOutput1, receiverOutput1);
        // reduce all
        Gf2eVoleReceiverOutput receiverOutputAll = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
        Gf2eVoleSenderOutput senderOutputAll = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        Gf2eVoleTestUtils.assertOutput(gf2e, num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce to num - 1
            Gf2eVoleReceiverOutput receiverOutputNum = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
            Gf2eVoleSenderOutput senderOutputNum = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            Gf2eVoleTestUtils.assertOutput(gf2e, num - 1, senderOutputNum, receiverOutputNum);
            // reduce to half
            Gf2eVoleReceiverOutput receiverOutputHalf = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
            Gf2eVoleSenderOutput senderOutputHalf = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            Gf2eVoleTestUtils.assertOutput(gf2e, num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        for (Gf2e gf2e : GF2ES) {
            testAllEmptyMerge(gf2e);
        }
    }

    private void testAllEmptyMerge(Gf2e gf2e) {
        byte[] delta = gf2e.createRangeRandom(SECURE_RANDOM);
        Gf2eVoleSenderOutput senderOutput = Gf2eVoleSenderOutput.createEmpty(gf2e);
        Gf2eVoleSenderOutput mergeSenderOutput = Gf2eVoleSenderOutput.createEmpty(gf2e);
        Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createEmpty(gf2e, delta);
        Gf2eVoleReceiverOutput mergeReceiverOutput = Gf2eVoleReceiverOutput.createEmpty(gf2e, delta);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        Gf2eVoleTestUtils.assertOutput(gf2e, 0, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (Gf2e gf2e : GF2ES) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testLeftEmptyMerge(gf2e, num);
            }
        }
    }

    private void testLeftEmptyMerge(Gf2e gf2e, int num) {
        byte[] delta = gf2e.createRangeRandom(SECURE_RANDOM);
        Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createEmpty(gf2e, delta);
        Gf2eVoleReceiverOutput mergeReceiverOutput = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
        Gf2eVoleSenderOutput senderOutput = Gf2eVoleSenderOutput.createEmpty(gf2e);
        Gf2eVoleSenderOutput mergeSenderOutput = Gf2eVoleTestUtils.genSenderOutput(gf2e, mergeReceiverOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        Gf2eVoleTestUtils.assertOutput(gf2e, num, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (Gf2e gf2e : GF2ES) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testRightEmptyMerge(gf2e, num);
            }
        }
    }

    private void testRightEmptyMerge(Gf2e gf2e, int num) {
        byte[] delta = gf2e.createRangeRandom(SECURE_RANDOM);
        Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
        Gf2eVoleReceiverOutput mergeReceiverOutput = Gf2eVoleReceiverOutput.createEmpty(gf2e, delta);
        Gf2eVoleSenderOutput senderOutput = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutput, SECURE_RANDOM);
        Gf2eVoleSenderOutput mergeSenderOutput = Gf2eVoleSenderOutput.createEmpty(gf2e);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        Gf2eVoleTestUtils.assertOutput(gf2e, num, senderOutput, receiverOutput);
    }

    @Test
    public void testMerge() {
        for (Gf2e gf2e : GF2ES) {
            for (int num1 = MIN_NUM; num1 < MAX_NUM; num1++) {
                for (int num2 = MIN_NUM; num2 < MAX_NUM; num2++) {
                    testMerge(gf2e, num1, num2);
                }
            }
        }
    }

    private void testMerge(Gf2e gf2e, int num1, int num2) {
        byte[] delta = gf2e.createRangeRandom(SECURE_RANDOM);
        Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num1, delta, SECURE_RANDOM);
        Gf2eVoleReceiverOutput mergeReceiverOutput = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num2, delta, SECURE_RANDOM);
        Gf2eVoleSenderOutput senderOutput = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutput, SECURE_RANDOM);
        Gf2eVoleSenderOutput mergeSenderOutput = Gf2eVoleTestUtils.genSenderOutput(gf2e, mergeReceiverOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        Gf2eVoleTestUtils.assertOutput(gf2e, num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (Gf2e gf2e : GF2ES) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testSplit(gf2e, num);
            }
        }
    }

    private void testSplit(Gf2e gf2e, int num) {
        byte[] delta = gf2e.createRangeRandom(SECURE_RANDOM);
        // split 1
        Gf2eVoleReceiverOutput receiverOutput1 = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
        Gf2eVoleSenderOutput senderOutput1 = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutput1, SECURE_RANDOM);
        Gf2eVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        Gf2eVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        Gf2eVoleTestUtils.assertOutput(gf2e, num - 1, senderOutput1, receiverOutput1);
        Gf2eVoleTestUtils.assertOutput(gf2e, 1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        Gf2eVoleReceiverOutput receiverOutputAll = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
        Gf2eVoleSenderOutput senderOutputAll = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutputAll, SECURE_RANDOM);
        Gf2eVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        Gf2eVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        Gf2eVoleTestUtils.assertOutput(gf2e, 0, senderOutputAll, receiverOutputAll);
        Gf2eVoleTestUtils.assertOutput(gf2e, num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            Gf2eVoleReceiverOutput receiverOutputNum = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
            Gf2eVoleSenderOutput senderOutputNum = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutputNum, SECURE_RANDOM);
            Gf2eVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            Gf2eVoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            Gf2eVoleTestUtils.assertOutput(gf2e, 1, senderOutputNum, receiverOutputNum);
            Gf2eVoleTestUtils.assertOutput(gf2e, num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // split half
            Gf2eVoleReceiverOutput receiverOutputHalf = Gf2eVoleTestUtils.genReceiverOutput(gf2e, num, delta, SECURE_RANDOM);
            Gf2eVoleSenderOutput senderOutputHalf = Gf2eVoleTestUtils.genSenderOutput(gf2e, receiverOutputHalf, SECURE_RANDOM);
            Gf2eVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            Gf2eVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            Gf2eVoleTestUtils.assertOutput(gf2e, num - num / 2, senderOutputHalf, receiverOutputHalf);
            Gf2eVoleTestUtils.assertOutput(gf2e, num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
