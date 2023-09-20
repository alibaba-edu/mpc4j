package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * COT output tests.
 *
 * @author Weiran Liu
 * @date 2022/4/11
 */
public class CotOutputTest {
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

    @Test
    public void testIllegalSenderInputs() {
        // create a sender output with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            CotSenderOutput.create(delta, new byte[0][]);
        });
        // create a sender output with short length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] r0Array = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(r0);
                    return r0;
                })
                .toArray(byte[][]::new);
            CotSenderOutput.create(delta, r0Array);
        });
        // create a sender output with long length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] r0Array = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(r0);
                    return r0;
                })
                .toArray(byte[][]::new);
            CotSenderOutput.create(delta, r0Array);
        });
        // create a sender output with short length r0
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] r0Array = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
                    SECURE_RANDOM.nextBytes(r0);
                    return r0;
                })
                .toArray(byte[][]::new);
            CotSenderOutput.create(delta, r0Array);
        });
        // create a sender output with long length r0
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] r0Array = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1];
                    SECURE_RANDOM.nextBytes(r0);
                    return r0;
                })
                .toArray(byte[][]::new);
            CotSenderOutput.create(delta, r0Array);
        });
        // merge two sender outputs with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta0);
            byte[][] r0Array0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(r0);
                    return r0;
                })
                .toArray(byte[][]::new);
            CotSenderOutput senderOutput0 = CotSenderOutput.create(delta0, r0Array0);
            byte[] delta1 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta1);
            byte[][] r0Array1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(r0);
                    return r0;
                })
                .toArray(byte[][]::new);
            CotSenderOutput senderOutput1 = CotSenderOutput.create(delta1, r0Array1);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testIllegalReceiverOutputs() {
        // create a receiver output with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            CotReceiverOutput.create(new boolean[0], new byte[0][])
        );
        // create a receiver output with mismatched num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            boolean[] choices = new boolean[MIN_NUM];
            IntStream.range(0, choices.length).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            CotReceiverOutput.create(choices, rbArray);
        });
        // create a receiver output with short length rb
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            boolean[] choices = new boolean[MAX_NUM];
            IntStream.range(0, choices.length).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            CotReceiverOutput.create(choices, rbArray);
        });
        // create a receiver output with long length rb
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            boolean[] choices = new boolean[MAX_NUM];
            IntStream.range(0, choices.length).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1];
                    SECURE_RANDOM.nextBytes(r0);
                    return r0;
                })
                .toArray(byte[][]::new);
            CotReceiverOutput.create(choices, rbArray);
        });
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        // reduce 1
        CotSenderOutput senderOutput1 = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutput1 = CotTestUtils.genReceiverOutput(senderOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        CotTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // reduce all
        CotSenderOutput senderOutputAll = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutputAll = CotTestUtils.genReceiverOutput(senderOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        CotTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            CotSenderOutput senderOutputNum = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
            CotReceiverOutput receiverOutputNum = CotTestUtils.genReceiverOutput(senderOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            CotTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // reduce half
            CotSenderOutput senderOutputHalf = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
            CotReceiverOutput receiverOutputHalf = CotTestUtils.genReceiverOutput(senderOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            CotTestUtils.assertOutput(num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        CotSenderOutput senderOutput = CotSenderOutput.createEmpty(delta);
        CotSenderOutput mergeSenderOutput = CotSenderOutput.createEmpty(delta);
        CotReceiverOutput receiverOutput = CotReceiverOutput.createEmpty();
        CotReceiverOutput mergeReceiverOutput = CotReceiverOutput.createEmpty();
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        CotTestUtils.assertOutput(0, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        CotSenderOutput senderOutput = CotSenderOutput.createEmpty(delta);
        CotSenderOutput mergeSenderOutput = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutput = CotReceiverOutput.createEmpty();
        CotReceiverOutput mergeReceiverOutput = CotTestUtils.genReceiverOutput(mergeSenderOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        CotTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        CotSenderOutput senderOutput = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotSenderOutput mergeSenderOutput = CotSenderOutput.createEmpty(delta);
        CotReceiverOutput receiverOutput = CotTestUtils.genReceiverOutput(senderOutput, SECURE_RANDOM);
        CotReceiverOutput mergeReceiverOutput = CotReceiverOutput.createEmpty();
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        CotTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        CotSenderOutput senderOutput = CotTestUtils.genSenderOutput(num1, delta, SECURE_RANDOM);
        CotSenderOutput mergeSenderOutput = CotTestUtils.genSenderOutput(num2, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutput = CotTestUtils.genReceiverOutput(senderOutput, SECURE_RANDOM);
        CotReceiverOutput mergeReceiverOutput = CotTestUtils.genReceiverOutput(mergeSenderOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        CotTestUtils.assertOutput(num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        // split 1
        CotSenderOutput senderOutput1 = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutput1 = CotTestUtils.genReceiverOutput(senderOutput1, SECURE_RANDOM);
        CotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        CotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        CotTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        CotTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        CotSenderOutput senderOutputAll = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutputAll = CotTestUtils.genReceiverOutput(senderOutputAll, SECURE_RANDOM);
        CotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        CotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        CotTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        CotTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            CotSenderOutput senderOutputNum = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
            CotReceiverOutput receiverOutputNum = CotTestUtils.genReceiverOutput(senderOutputNum, SECURE_RANDOM);
            CotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            CotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            CotTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            CotTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputNum);
            // split half
            CotSenderOutput senderOutputHalf = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
            CotReceiverOutput receiverOutputHalf = CotTestUtils.genReceiverOutput(senderOutputHalf, SECURE_RANDOM);
            CotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            CotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            CotTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            CotTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
