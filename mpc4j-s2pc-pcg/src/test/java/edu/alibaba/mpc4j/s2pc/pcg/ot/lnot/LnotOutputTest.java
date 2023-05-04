package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * 1-out-of-n OT test, where n = 2^l.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class LnotOutputTest {
    /**
     * minimal num
     */
    private static final int MIN_NUM = 1;
    /**
     * maximal num
     */
    private static final int MAX_NUM = 64;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default l
     */
    private static final int DEFAULT_L = 5;
    /**
     * default n
     */
    private static final int DEFAULT_N = 1 << DEFAULT_L;

    @Test
    public void testIllegalSenderOutputs() {
        // create a sender output with num = 0
        Assert.assertThrows(AssertionError.class, () -> LnotSenderOutput.create(DEFAULT_L, new byte[0][][]));
        // create a sender output with short rs length
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            LnotSenderOutput.create(DEFAULT_L, rsArray);
        });
        // create a sender output with large rs length
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            LnotSenderOutput.create(DEFAULT_L, rsArray);
        });
        // create a sender output with less rs
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N - 1)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            LnotSenderOutput.create(DEFAULT_L, rsArray);
        });
        // create a sender output with more rs
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N + 1)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            LnotSenderOutput.create(DEFAULT_L, rsArray);
        });
        // merge two sender output with different l
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            LnotSenderOutput senderOutput0 = LnotSenderOutput.create(DEFAULT_L, rsArray0);
            byte[][][] rsArray1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, 1 << (DEFAULT_L + 1))
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            LnotSenderOutput senderOutput1 = LnotSenderOutput.create(DEFAULT_L + 1, rsArray1);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testIllegalReceiverOutputs() {
        // create a receiver output with num = 0
        Assert.assertThrows(AssertionError.class, () -> LnotReceiverOutput.create(DEFAULT_L, new int[0], new byte[0][]));
        // create a receiver output with mismatched num
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MIN_NUM)
                .map(index -> SECURE_RANDOM.nextInt(DEFAULT_N))
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            LnotReceiverOutput.create(DEFAULT_L, choices, rbArray);
        });
        // create a receiver with negative choice
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> -1)
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            LnotReceiverOutput.create(DEFAULT_L, choices, rbArray);
        });
        // create a receiver with large choice
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> DEFAULT_N)
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            LnotReceiverOutput.create(DEFAULT_L, choices, rbArray);
        });
        // create a receiver output with less rb
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> SECURE_RANDOM.nextInt(DEFAULT_N))
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            LnotReceiverOutput.create(DEFAULT_L, choices, rbArray);
        });
        // create a receiver output with more rb
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> SECURE_RANDOM.nextInt(DEFAULT_N))
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            LnotReceiverOutput.create(DEFAULT_L, choices, rbArray);
        });
        // merge two receiver output with different l
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices0 = IntStream.range(0, MAX_NUM)
                .map(index -> SECURE_RANDOM.nextInt(DEFAULT_N))
                .toArray();
            byte[][] rbArray0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            LnotReceiverOutput receiverOutput0 = LnotReceiverOutput.create(DEFAULT_L, choices0, rbArray0);
            int[] choices1 = IntStream.range(0, MAX_NUM)
                .map(index -> SECURE_RANDOM.nextInt(1 << (DEFAULT_L + 1)))
                .toArray();
            byte[][] rbArray1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            LnotReceiverOutput receiverOutput1 = LnotReceiverOutput.create(DEFAULT_L + 1, choices1, rbArray1);
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
        // reduce 1
        LnotSenderOutput senderOutput1 = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
        LnotReceiverOutput receiverOutput1 = LnotTestUtils.genReceiverOutput(senderOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        LnotTestUtils.assertOutput(DEFAULT_L, 1, senderOutput1, receiverOutput1);
        // reduce all
        LnotSenderOutput senderOutputAll = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
        LnotReceiverOutput receiverOutputAll = LnotTestUtils.genReceiverOutput(senderOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        LnotTestUtils.assertOutput(DEFAULT_L, num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            LnotSenderOutput senderOutputNum = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
            LnotReceiverOutput receiverOutputNum = LnotTestUtils.genReceiverOutput(senderOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            LnotTestUtils.assertOutput(DEFAULT_L, num - 1, senderOutputNum, receiverOutputNum);
            // reduce half
            LnotSenderOutput senderOutputHalf = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
            LnotReceiverOutput receiverOutputHalf = LnotTestUtils.genReceiverOutput(senderOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            LnotTestUtils.assertOutput(DEFAULT_L, num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        LnotSenderOutput senderOutput = LnotSenderOutput.createEmpty(DEFAULT_L);
        LnotSenderOutput mergeSenderOutput = LnotSenderOutput.createEmpty(DEFAULT_L);
        LnotReceiverOutput receiverOutput = LnotReceiverOutput.createEmpty(DEFAULT_L);
        LnotReceiverOutput mergeReceiverOutput = LnotReceiverOutput.createEmpty(DEFAULT_L);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        LnotTestUtils.assertOutput(DEFAULT_L, 0, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        LnotSenderOutput senderOutput = LnotSenderOutput.createEmpty(DEFAULT_L);
        LnotSenderOutput mergeSenderOutput = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
        LnotReceiverOutput receiverOutput = LnotReceiverOutput.createEmpty(DEFAULT_L);
        LnotReceiverOutput mergeReceiverOutput = LnotTestUtils.genReceiverOutput(mergeSenderOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        LnotTestUtils.assertOutput(DEFAULT_L, num, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        LnotSenderOutput senderOutput = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
        LnotSenderOutput mergeSenderOutput = LnotSenderOutput.createEmpty(DEFAULT_L);
        LnotReceiverOutput receiverOutput = LnotTestUtils.genReceiverOutput(senderOutput, SECURE_RANDOM);
        LnotReceiverOutput mergeReceiverOutput = LnotReceiverOutput.createEmpty(DEFAULT_L);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        LnotTestUtils.assertOutput(DEFAULT_L, num, senderOutput, receiverOutput);
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
        LnotSenderOutput senderOutput = LnotTestUtils.genSenderOutput(DEFAULT_L, num1, SECURE_RANDOM);
        LnotSenderOutput mergeSenderOutput = LnotTestUtils.genSenderOutput(DEFAULT_L, num2, SECURE_RANDOM);
        LnotReceiverOutput receiverOutput = LnotTestUtils.genReceiverOutput(senderOutput, SECURE_RANDOM);
        LnotReceiverOutput mergeReceiverOutput = LnotTestUtils.genReceiverOutput(mergeSenderOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        LnotTestUtils.assertOutput(DEFAULT_L, num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        // split 1
        LnotSenderOutput senderOutput1 = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
        LnotReceiverOutput receiverOutput1 = LnotTestUtils.genReceiverOutput(senderOutput1, SECURE_RANDOM);
        LnotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        LnotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        LnotTestUtils.assertOutput(DEFAULT_L, num - 1, senderOutput1, receiverOutput1);
        LnotTestUtils.assertOutput(DEFAULT_L, 1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        LnotSenderOutput senderOutputAll = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
        LnotReceiverOutput receiverOutputAll = LnotTestUtils.genReceiverOutput(senderOutputAll, SECURE_RANDOM);
        LnotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        LnotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        LnotTestUtils.assertOutput(DEFAULT_L, 0, senderOutputAll, receiverOutputAll);
        LnotTestUtils.assertOutput(DEFAULT_L, num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            LnotSenderOutput senderOutputNum = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
            LnotReceiverOutput receiverOutputNum = LnotTestUtils.genReceiverOutput(senderOutputNum, SECURE_RANDOM);
            LnotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            LnotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            LnotTestUtils.assertOutput(DEFAULT_L, 1, senderOutputNum, receiverOutputNum);
            LnotTestUtils.assertOutput(DEFAULT_L, num - 1, splitSenderOutputNum, splitReceiverOutputNum);
            // split half
            LnotSenderOutput senderOutputHalf = LnotTestUtils.genSenderOutput(DEFAULT_L, num, SECURE_RANDOM);
            LnotReceiverOutput receiverOutputHalf = LnotTestUtils.genReceiverOutput(senderOutputHalf, SECURE_RANDOM);
            LnotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            LnotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            LnotTestUtils.assertOutput(DEFAULT_L, num - num / 2, senderOutputHalf, receiverOutputHalf);
            LnotTestUtils.assertOutput(DEFAULT_L, num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
