package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

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
    private final SecureRandom secureRandom;

    public CotOutputTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalSenderInputs() {
        // create a sender output with short length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH - 1, secureRandom);
            byte[][] r0Array = BlockUtils.randomBlocks(MAX_NUM, secureRandom);
            CotSenderOutput.create(delta, r0Array);
        });
        // create a sender output with long length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH + 1, secureRandom);
            byte[][] r0Array = BlockUtils.randomBlocks(MAX_NUM, secureRandom);
            CotSenderOutput.create(delta, r0Array);
        });
        // create a sender output with short length r0
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BlockUtils.randomBlock(secureRandom);
            byte[][] r0Array = BytesUtils.randomByteArrayVector(MAX_NUM, CommonConstants.BLOCK_BYTE_LENGTH - 1, secureRandom);
            CotSenderOutput.create(delta, r0Array);
        });
        // create a sender output with long length r0
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BlockUtils.randomBlock(secureRandom);
            byte[][] r0Array = BytesUtils.randomByteArrayVector(MAX_NUM, CommonConstants.BLOCK_BYTE_LENGTH + 1, secureRandom);
            CotSenderOutput.create(delta, r0Array);
        });
    }

    @Test
    public void testIllegalReceiverInputs() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] rbArray = BlockUtils.randomBlocks(MAX_NUM, secureRandom);
            CotReceiverOutput.create(new boolean[0], rbArray);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            boolean[] choices = BinaryUtils.randomBinary(MAX_NUM, secureRandom);
            CotReceiverOutput.create(choices, BlockUtils.randomBlocks(0, secureRandom));
        });
        // create a receiver output with mismatched num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            boolean[] choices = BinaryUtils.randomBinary(MIN_NUM, secureRandom);
            byte[][] rbArray = BlockUtils.randomBlocks(MAX_NUM, secureRandom);
            CotReceiverOutput.create(choices, rbArray);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            boolean[] choices = BinaryUtils.randomBinary(MAX_NUM, secureRandom);
            byte[][] rbArray = BlockUtils.randomBlocks(MIN_NUM, secureRandom);
            CotReceiverOutput.create(choices, rbArray);
        });
        // create a receiver output with short length rb
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            boolean[] choices = BinaryUtils.randomBinary(MAX_NUM, secureRandom);
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, CommonConstants.BLOCK_BYTE_LENGTH - 1, secureRandom);
            CotReceiverOutput.create(choices, rbArray);
        });
        // create a receiver output with long length rb
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            boolean[] choices = BinaryUtils.randomBinary(MAX_NUM, secureRandom);
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, CommonConstants.BLOCK_BYTE_LENGTH + 1, secureRandom);
            CotReceiverOutput.create(choices, rbArray);
        });
    }

    @Test
    public void testIllegalUpdate() {
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CotSenderOutput senderOutput = CotSenderOutput.createRandom(4, delta, secureRandom);
            senderOutput.split(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CotSenderOutput senderOutput = CotSenderOutput.createRandom(4, delta, secureRandom);
            CotReceiverOutput receiverOutput = CotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CotSenderOutput senderOutput = CotSenderOutput.createRandom(4, delta, secureRandom);
            senderOutput.split(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CotSenderOutput senderOutput = CotSenderOutput.createRandom(4, delta, secureRandom);
            CotReceiverOutput receiverOutput = CotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CotSenderOutput senderOutput = CotSenderOutput.createRandom(4, delta, secureRandom);
            senderOutput.reduce(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CotSenderOutput senderOutput = CotSenderOutput.createRandom(4, delta, secureRandom);
            CotReceiverOutput receiverOutput = CotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CotSenderOutput senderOutput = CotSenderOutput.createRandom(4, delta, secureRandom);
            senderOutput.reduce(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            CotSenderOutput senderOutput = CotSenderOutput.createRandom(4, delta, secureRandom);
            CotReceiverOutput receiverOutput = CotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.reduce(5);
        });
        // merge two sender outputs with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta0 = BlockUtils.randomBlock(secureRandom);
            CotSenderOutput senderOutput0 = CotSenderOutput.createRandom(4, delta0, secureRandom);
            byte[] delta1 = BlockUtils.randomBlock(secureRandom);
            CotSenderOutput senderOutput1 = CotSenderOutput.createRandom(4, delta1, secureRandom);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        CotSenderOutput senderOutput = CotSenderOutput.createRandom(num, delta, secureRandom);
        CotReceiverOutput receiverOutput = CotReceiverOutput.createRandom(senderOutput, secureRandom);
        OtTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        // reduce 1
        CotSenderOutput senderOutput1 = CotSenderOutput.createRandom(num, delta, secureRandom);
        CotReceiverOutput receiverOutput1 = CotReceiverOutput.createRandom(senderOutput1, secureRandom);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        OtTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // reduce all
        CotSenderOutput senderOutputAll = CotSenderOutput.createRandom(num, delta, secureRandom);
        CotReceiverOutput receiverOutputAll = CotReceiverOutput.createRandom(senderOutputAll, secureRandom);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        OtTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            CotSenderOutput senderOutputNum = CotSenderOutput.createRandom(num, delta, secureRandom);
            CotReceiverOutput receiverOutputNum = CotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            OtTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // reduce half
            CotSenderOutput senderOutputHalf = CotSenderOutput.createRandom(num, delta, secureRandom);
            CotReceiverOutput receiverOutputHalf = CotReceiverOutput.createRandom(senderOutputHalf, secureRandom);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            OtTestUtils.assertOutput(num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testMerge() {
        for (int num1 = 0; num1 < MAX_NUM; num1++) {
            for (int num2 = 0; num2 < MAX_NUM; num2++) {
                testMerge(num1, num2);
            }
        }
    }

    private void testMerge(int num1, int num2) {
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        CotSenderOutput senderOutput = CotSenderOutput.createRandom(num1, delta, secureRandom);
        CotSenderOutput mergeSenderOutput = CotSenderOutput.createRandom(num2, delta, secureRandom);
        CotReceiverOutput receiverOutput = CotReceiverOutput.createRandom(senderOutput, secureRandom);
        CotReceiverOutput mergeReceiverOutput = CotReceiverOutput.createRandom(mergeSenderOutput, secureRandom);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        OtTestUtils.assertOutput(num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        // split 1
        CotSenderOutput senderOutput1 = CotSenderOutput.createRandom(num, delta, secureRandom);
        CotReceiverOutput receiverOutput1 = CotReceiverOutput.createRandom(senderOutput1, secureRandom);
        CotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        CotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        OtTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        OtTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        CotSenderOutput senderOutputAll = CotSenderOutput.createRandom(num, delta, secureRandom);
        CotReceiverOutput receiverOutputAll = CotReceiverOutput.createRandom(senderOutputAll, secureRandom);
        CotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        CotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        OtTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        OtTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            CotSenderOutput senderOutputNum = CotSenderOutput.createRandom(num, delta, secureRandom);
            CotReceiverOutput receiverOutputNum = CotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            CotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            CotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            OtTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            OtTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputNum);
            // split half
            CotSenderOutput senderOutputHalf = CotSenderOutput.createRandom(num, delta, secureRandom);
            CotReceiverOutput receiverOutputHalf = CotReceiverOutput.createRandom(senderOutputHalf, secureRandom);
            CotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            CotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            OtTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            OtTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }

    @Test
    public void testSplitMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplitMerge(num);
        }
    }

    private void testSplitMerge(int num) {
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        // split and merge 1
        CotSenderOutput senderOutput1 = CotSenderOutput.createRandom(num, delta, secureRandom);
        CotSenderOutput copySenderOutput1 = senderOutput1.copy();
        CotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        senderOutput1.merge(splitSenderOutput1);
        Assert.assertEquals(copySenderOutput1, senderOutput1);
        CotReceiverOutput receiverOutput1 = CotReceiverOutput.createRandom(senderOutput1, secureRandom);
        CotReceiverOutput copyReceiverOutput1 = receiverOutput1.copy();
        CotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        receiverOutput1.merge(splitReceiverOutput1);
        Assert.assertEquals(copyReceiverOutput1, receiverOutput1);
        // split and merge all
        CotSenderOutput senderOutputAll = CotSenderOutput.createRandom(num, delta, secureRandom);
        CotSenderOutput copySenderOutputAll = senderOutputAll.copy();
        CotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        senderOutputAll.merge(splitSenderOutputAll);
        Assert.assertEquals(copySenderOutputAll, senderOutputAll);
        CotReceiverOutput receiverOutputAll = CotReceiverOutput.createRandom(senderOutputAll, secureRandom);
        CotReceiverOutput copyReceiverOutputAll = receiverOutputAll.copy();
        CotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        receiverOutputAll.merge(splitReceiverOutputAll);
        Assert.assertEquals(copyReceiverOutputAll, receiverOutputAll);
        if (num > 1) {
            // split and merge num - 1
            CotSenderOutput senderOutputNum = CotSenderOutput.createRandom(num, delta, secureRandom);
            CotSenderOutput copySenderOutputNum = senderOutputNum.copy();
            CotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            senderOutputNum.merge(splitSenderOutputNum);
            Assert.assertEquals(copySenderOutputNum, senderOutputNum);
            CotReceiverOutput receiverOutputNum = CotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            CotReceiverOutput copyReceiverOutputNum = receiverOutputNum.copy();
            CotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            receiverOutputNum.merge(splitReceiverOutputNum);
            Assert.assertEquals(copyReceiverOutputNum, receiverOutputNum);
            // split half
            CotSenderOutput senderOutputHalf = CotSenderOutput.createRandom(num, delta, secureRandom);
            CotSenderOutput copySenderOutputHalf = senderOutputHalf.copy();
            CotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            senderOutputHalf.merge(splitSenderOutputHalf);
            Assert.assertEquals(copySenderOutputHalf, senderOutputHalf);
            CotReceiverOutput receiverOutputHalf = CotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            CotReceiverOutput copyReceiverOutputHalf = receiverOutputHalf.copy();
            CotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            receiverOutputHalf.merge(splitReceiverOutputHalf);
            Assert.assertEquals(copyReceiverOutputHalf, receiverOutputHalf);
        }
    }
}
