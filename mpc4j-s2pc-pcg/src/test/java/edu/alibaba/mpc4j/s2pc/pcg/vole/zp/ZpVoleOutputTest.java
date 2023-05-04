package edu.alibaba.mpc4j.s2pc.pcg.vole.zp;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * ZP-VOLE output tests.
 *
 * @author Weiran Liu
 * @date 2022/6/14
 */
public class ZpVoleOutputTest {
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
     * the default Zp instance
     */
    private static final Zp DEFAULT_ZP = ZpFactory.createInstance(EnvType.STANDARD, ZpManager.getPrime(64));
    /**
     * another Zp instance
     */
    private static final Zp OTHER_ZP = ZpFactory.createInstance(EnvType.STANDARD, ZpManager.getPrime(65));

    @Test
    public void testSenderIllegalInputs() {
        // create a sender output with length 0
        Assert.assertThrows(AssertionError.class, () ->
            ZpVoleSenderOutput.create(DEFAULT_ZP, new BigInteger[0], new BigInteger[0])
        );
        // create a sender output with mismatched length
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] x = IntStream.range(0, MIN_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(DEFAULT_ZP, x, t);
        });
        // create a sender output with negative x
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createNonZeroRandom(SECURE_RANDOM).negate())
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(DEFAULT_ZP, x, t);
        });
        // create a sender output with large x
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.getPrime().add(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(DEFAULT_ZP, x, t);
        });
        // create a sender output with negative t
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createNonZeroRandom(SECURE_RANDOM).negate())
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(DEFAULT_ZP, x, t);
        });
        // create a sender output with large t
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.getPrime().add(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(DEFAULT_ZP, x, t);
        });
        // merge two sender outputs with different p
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] x0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] t0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput senderOutput0 = ZpVoleSenderOutput.create(DEFAULT_ZP, x0, t0);
            BigInteger[] x1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> OTHER_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] t1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> OTHER_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput senderOutput1 = ZpVoleSenderOutput.create(DEFAULT_ZP, x1, t1);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testReceiverIllegalInputs() {
        // create a receiver output with length = 0
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger delta = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
            ZpVoleReceiverOutput.create(DEFAULT_ZP, delta, new BigInteger[0]);
        });
        // create a receiver output with a negative Δ
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger delta = BigInteger.ONE.negate();
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(DEFAULT_ZP, delta, q);
        });
        // create a receiver output with invalid Δ
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger delta = DEFAULT_ZP.getPrime().subtract(BigInteger.ONE);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(DEFAULT_ZP, delta, q);
        });
        // create a receiver output with large Δ
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger delta = DEFAULT_ZP.getPrime().add(BigInteger.ONE);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(DEFAULT_ZP, delta, q);
        });
        // create a receiver output with negative q
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger delta = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createNonZeroRandom(SECURE_RANDOM).negate())
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(DEFAULT_ZP, delta, q);
        });
        // create a receiver output with large q
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger delta = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.getPrime().add(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(DEFAULT_ZP, delta, q);
        });
        // merge two receiver outputs with different Δ
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger delta0 = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
            BigInteger delta1 = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput receiverOutput0 = ZpVoleReceiverOutput.create(DEFAULT_ZP, delta0, q);
            ZpVoleReceiverOutput receiverOutput1 = ZpVoleReceiverOutput.create(DEFAULT_ZP, delta1, q);
            receiverOutput0.merge(receiverOutput1);
        });
        // merge two receiver outputs with different Zp
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger delta0 = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
            BigInteger[] q0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> DEFAULT_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput receiverOutput0 = ZpVoleReceiverOutput.create(DEFAULT_ZP, delta0, q0);
            BigInteger delta1 = OTHER_ZP.createRangeRandom(SECURE_RANDOM);
            BigInteger[] q1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> OTHER_ZP.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput receiverOutput1 = ZpVoleReceiverOutput.create(OTHER_ZP, delta1, q1);
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
        BigInteger delta = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
        // reduce 1
        ZpVoleReceiverOutput receiverOutput1 = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput1 = ZpVoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        ZpVoleTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // reduce all
        ZpVoleReceiverOutput receiverOutputAll = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutputAll = ZpVoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        ZpVoleTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            ZpVoleReceiverOutput receiverOutputNum = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
            ZpVoleSenderOutput senderOutputNum = ZpVoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            ZpVoleTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // reduce half
            ZpVoleReceiverOutput receiverOutputHalf = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
            ZpVoleSenderOutput senderOutputHalf = ZpVoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            ZpVoleTestUtils.assertOutput(num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        BigInteger delta = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createEmpty(DEFAULT_ZP);
        ZpVoleSenderOutput mergeSenderOutput = ZpVoleSenderOutput.createEmpty(DEFAULT_ZP);
        ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createEmpty(DEFAULT_ZP, delta);
        ZpVoleReceiverOutput mergeReceiverOutput = ZpVoleReceiverOutput.createEmpty(DEFAULT_ZP, delta);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        ZpVoleTestUtils.assertOutput(0, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        BigInteger delta = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
        ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createEmpty(DEFAULT_ZP, delta);
        ZpVoleReceiverOutput mergeReceiverOutput = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createEmpty(DEFAULT_ZP);
        ZpVoleSenderOutput mergeSenderOutput = ZpVoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        ZpVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        BigInteger delta = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
        ZpVoleReceiverOutput receiverOutput = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
        ZpVoleReceiverOutput mergeReceiverOutput = ZpVoleReceiverOutput.createEmpty(DEFAULT_ZP, delta);
        ZpVoleSenderOutput senderOutput = ZpVoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        ZpVoleSenderOutput mergeSenderOutput = ZpVoleSenderOutput.createEmpty(DEFAULT_ZP);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        ZpVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        BigInteger delta = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
        ZpVoleReceiverOutput receiverOutput = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num1, delta, SECURE_RANDOM);
        ZpVoleReceiverOutput mergeReceiverOutput = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num2, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput = ZpVoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        ZpVoleSenderOutput mergeSenderOutput = ZpVoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        ZpVoleTestUtils.assertOutput(num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        BigInteger delta = DEFAULT_ZP.createRangeRandom(SECURE_RANDOM);
        // split 1
        ZpVoleReceiverOutput receiverOutput1 = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput1 = ZpVoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        ZpVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        ZpVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        ZpVoleTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        ZpVoleTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        ZpVoleReceiverOutput receiverOutputAll = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutputAll = ZpVoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        ZpVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        ZpVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        ZpVoleTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        ZpVoleTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            ZpVoleReceiverOutput receiverOutputNum = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
            ZpVoleSenderOutput senderOutputNum = ZpVoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            ZpVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            ZpVoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            ZpVoleTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            ZpVoleTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // split half
            ZpVoleReceiverOutput receiverOutputHalf = ZpVoleTestUtils.genReceiverOutput(DEFAULT_ZP, num, delta, SECURE_RANDOM);
            ZpVoleSenderOutput senderOutputHalf = ZpVoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            ZpVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            ZpVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            ZpVoleTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            ZpVoleTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
