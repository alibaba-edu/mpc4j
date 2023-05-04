package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zp64-VOLE tests.
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/6/15
 */
public class Zp64VoleOutputTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 64;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * the default Zp64 instance
     */
    private static final Zp64 DEFAULT_ZP64 = Zp64Factory.createInstance(EnvType.STANDARD, 62);
    /**
     * the other Zp64 instance
     */
    private static final Zp64 OTHER_ZP64 = Zp64Factory.createInstance(EnvType.STANDARD, 32);

    @Test
    public void testSenderIllegalOutputs() {
        // create a sender output with length 0
        Assert.assertThrows(AssertionError.class, () ->
            Zp64VoleSenderOutput.create(DEFAULT_ZP64, new long[0], new long[0])
        );
        // create a sender output with mismatched length
        Assert.assertThrows(AssertionError.class, () -> {
            long[] x = IntStream.range(0, MIN_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleSenderOutput.create(DEFAULT_ZP64, x, t);
        });
        // create a sender output with negative x
        Assert.assertThrows(AssertionError.class, () -> {
            long[] x = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> -DEFAULT_ZP64.createNonZeroRandom(SECURE_RANDOM))
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleSenderOutput.create(DEFAULT_ZP64, x, t);
        });
        // create a sender output with large x
        Assert.assertThrows(AssertionError.class, () -> {
            long[] x = IntStream.range(0, MIN_NUM)
                .mapToLong(index -> DEFAULT_ZP64.getPrime() + 1L)
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleSenderOutput.create(DEFAULT_ZP64, x, t);
        });
        // create a sender output with negative t
        Assert.assertThrows(AssertionError.class, () -> {
            long[] x = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> -DEFAULT_ZP64.createNonZeroRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleSenderOutput.create(DEFAULT_ZP64, x, t);
        });
        // create a sender output with large t
        Assert.assertThrows(AssertionError.class, () -> {
            long[] x = IntStream.range(0, MIN_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.getPrime() + 1L)
                .toArray();
            Zp64VoleSenderOutput.create(DEFAULT_ZP64, x, t);
        });
        // merge two sender outputs with different p
        Assert.assertThrows(AssertionError.class, () -> {
            long[] x0 = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            long[] t0 = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleSenderOutput senderOutput0 = Zp64VoleSenderOutput.create(DEFAULT_ZP64, x0, t0);
            long[] x1 = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> OTHER_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            long[] t1 = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> OTHER_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleSenderOutput senderOutput1 = Zp64VoleSenderOutput.create(OTHER_ZP64, x1, t1);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testReceiverIllegalInputs() {
        // create a receiver output with length = 0
        Assert.assertThrows(AssertionError.class, () -> {
            long delta = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
            Zp64VoleReceiverOutput.create(DEFAULT_ZP64, delta, new long[0]);
        });
        // create a receiver output with a negative Δ
        Assert.assertThrows(AssertionError.class, () -> {
            long delta = -1L;
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleReceiverOutput.create(DEFAULT_ZP64, delta, q);
        });
        // create a receiver output with invalid Δ
        Assert.assertThrows(AssertionError.class, () -> {
            long delta = DEFAULT_ZP64.getPrime() - 1L;
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleReceiverOutput.create(DEFAULT_ZP64, delta, q);
        });
        // create a receiver output with large Δ
        Assert.assertThrows(AssertionError.class, () -> {
            long delta = DEFAULT_ZP64.getPrime() + 1L;
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleReceiverOutput.create(DEFAULT_ZP64, delta, q);
        });
        // create a receiver output with negative q
        Assert.assertThrows(AssertionError.class, () -> {
            long delta = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> -DEFAULT_ZP64.createNonZeroRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleReceiverOutput.create(DEFAULT_ZP64, delta, q);
        });
        // create a receiver output with large q
        Assert.assertThrows(AssertionError.class, () -> {
            long delta = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.getPrime() + 1L)
                .toArray();
            Zp64VoleReceiverOutput.create(DEFAULT_ZP64, delta, q);
        });
        // merge two receiver outputs with different Δ
        Assert.assertThrows(AssertionError.class, () -> {
            long delta0 = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
            long delta1 = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleReceiverOutput receiverOutput0 = Zp64VoleReceiverOutput.create(DEFAULT_ZP64, delta0, q);
            Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleReceiverOutput.create(DEFAULT_ZP64, delta1, q);
            receiverOutput0.merge(receiverOutput1);
        });
        // merge two receiver outputs with different Zp
        Assert.assertThrows(AssertionError.class, () -> {
            long delta0 = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
            long[] q0 = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> DEFAULT_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleReceiverOutput receiverOutput0 = Zp64VoleReceiverOutput.create(DEFAULT_ZP64, delta0, q0);
            long delta1 = OTHER_ZP64.createRangeRandom(SECURE_RANDOM);
            long[] q1 = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> OTHER_ZP64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleReceiverOutput.create(OTHER_ZP64, delta1, q1);
            receiverOutput0.merge(receiverOutput1);
        });
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        long delta = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
        // reduce 1
        Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput1 = Zp64VoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        Zp64VoleTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // reduce all
        Zp64VoleReceiverOutput receiverOutputAll = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutputAll = Zp64VoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        Zp64VoleTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            Zp64VoleReceiverOutput receiverOutputNum = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
            Zp64VoleSenderOutput senderOutputNum = Zp64VoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            Zp64VoleTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // reduce half
            Zp64VoleReceiverOutput receiverOutputHalf = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
            Zp64VoleSenderOutput senderOutputHalf = Zp64VoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            Zp64VoleTestUtils.assertOutput(num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        long delta = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createEmpty(DEFAULT_ZP64);
        Zp64VoleSenderOutput mergeSenderOutput = Zp64VoleSenderOutput.createEmpty(DEFAULT_ZP64);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createEmpty(DEFAULT_ZP64, delta);
        Zp64VoleReceiverOutput mergeReceiverOutput = Zp64VoleReceiverOutput.createEmpty(DEFAULT_ZP64, delta);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        Zp64VoleTestUtils.assertOutput(0, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        long delta = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createEmpty(DEFAULT_ZP64, delta);
        Zp64VoleReceiverOutput mergeReceiverOutput = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createEmpty(DEFAULT_ZP64);
        Zp64VoleSenderOutput mergeSenderOutput = Zp64VoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        Zp64VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        long delta = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
        Zp64VoleReceiverOutput mergeReceiverOutput = Zp64VoleReceiverOutput.createEmpty(DEFAULT_ZP64, delta);
        Zp64VoleSenderOutput senderOutput = Zp64VoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        Zp64VoleSenderOutput mergeSenderOutput = Zp64VoleSenderOutput.createEmpty(DEFAULT_ZP64);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        Zp64VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testMerge() {
        for (int num1 = MIN_NUM; num1 < MAX_NUM; num1++) {
            for (int num2 = MIN_NUM; num2 < MAX_NUM; num2++) {
                testMerge(num1, num2);
            }
        }
    }

    private void testMerge(int num1, int num2) {
        long delta = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num1, delta, SECURE_RANDOM);
        Zp64VoleReceiverOutput mergeReceiverOutput = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num2, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput = Zp64VoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        Zp64VoleSenderOutput mergeSenderOutput = Zp64VoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        Zp64VoleTestUtils.assertOutput(num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        long delta = DEFAULT_ZP64.createRangeRandom(SECURE_RANDOM);
        // split 1
        Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput1 = Zp64VoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        Zp64VoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        Zp64VoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        Zp64VoleTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        Zp64VoleTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        Zp64VoleReceiverOutput receiverOutputAll = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutputAll = Zp64VoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        Zp64VoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        Zp64VoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        Zp64VoleTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        Zp64VoleTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            Zp64VoleReceiverOutput receiverOutputNum = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
            Zp64VoleSenderOutput senderOutputNum = Zp64VoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            Zp64VoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            Zp64VoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            Zp64VoleTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            Zp64VoleTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // split half
            Zp64VoleReceiverOutput receiverOutputHalf = Zp64VoleTestUtils.genReceiverOutput(DEFAULT_ZP64, num, delta, SECURE_RANDOM);
            Zp64VoleSenderOutput senderOutputHalf = Zp64VoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            Zp64VoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            Zp64VoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            Zp64VoleTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            Zp64VoleTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
