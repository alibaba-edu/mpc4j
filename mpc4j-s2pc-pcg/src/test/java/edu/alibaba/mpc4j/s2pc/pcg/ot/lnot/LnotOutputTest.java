package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtTestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * 1-out-of-n OT output test, where n = 2^l.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
@RunWith(Parameterized.class)
public class LnotOutputTest {
    /**
     * minimal num
     */
    private static final int MIN_NUM = 1;
    /**
     * maximal num
     */
    private static final int MAX_NUM = 64;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int l : new int[] {1, 3, 8}) {
            configurations.add(new Object[] {"l = " + l, l});
        }

        return configurations;
    }

    /**
     * l
     */
    private final int l;
    /**
     * n = 2^l
     */
    private final int n;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public LnotOutputTest(String name, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.l = l;
        this.n = 1 << l;
        this.secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalSenderInputs() {
        // create a sender output with short rs length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    BytesUtils.randomByteArrayVector(n, CommonConstants.BLOCK_BYTE_LENGTH - 1, secureRandom)
                )
                .toArray(byte[][][]::new);
            LnotSenderOutput.create(l, rsArray);
        });
        // create a sender output with large rs length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    BytesUtils.randomByteArrayVector(n, CommonConstants.BLOCK_BYTE_LENGTH + 1, secureRandom)
                )
                .toArray(byte[][][]::new);
            LnotSenderOutput.create(l, rsArray);
        });
        // create a sender output with less rs
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    BytesUtils.randomByteArrayVector(n - 1, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom)
                )
                .toArray(byte[][][]::new);
            LnotSenderOutput.create(l, rsArray);
        });
        // create a sender output with more rs
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    BytesUtils.randomByteArrayVector(n + 1, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom)
                )
                .toArray(byte[][][]::new);
            LnotSenderOutput.create(l, rsArray);
        });
    }

    @Test
    public void testIllegalReceiverInputs() {
        // create a receiver output with mismatched num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] choices = IntStream.range(0, MIN_NUM)
                .map(index -> IntUtils.randomNonNegative(n, secureRandom))
                .toArray();
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
            LnotReceiverOutput.create(l, choices, rbArray);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> IntUtils.randomNonNegative(n, secureRandom))
                .toArray();
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MIN_NUM, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
            LnotReceiverOutput.create(l, choices, rbArray);
        });
        // create a receiver with negative choice
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> -1)
                .toArray();
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MIN_NUM, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
            LnotReceiverOutput.create(l, choices, rbArray);
        });
        // create a receiver with large choice
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> n)
                .toArray();
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MIN_NUM, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
            LnotReceiverOutput.create(l, choices, rbArray);
        });
        // create a receiver output with mis-matched num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> IntUtils.randomNonNegative(n, secureRandom))
                .toArray();
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MIN_NUM, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
            LnotReceiverOutput.create(l, choices, rbArray);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] choices = IntStream.range(0, MIN_NUM)
                .map(index -> IntUtils.randomNonNegative(n, secureRandom))
                .toArray();
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
            LnotReceiverOutput.create(l, choices, rbArray);
        });
        // create a receiver output with short rb
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> IntUtils.randomNonNegative(n, secureRandom))
                .toArray();
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, CommonConstants.BLOCK_BYTE_LENGTH - 1, secureRandom);
            LnotReceiverOutput.create(l, choices, rbArray);
        });
        // create a receiver output with large rb
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> IntUtils.randomNonNegative(n, secureRandom))
                .toArray();
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, CommonConstants.BLOCK_BYTE_LENGTH + 1, secureRandom);
            LnotReceiverOutput.create(l, choices, rbArray);
        });
    }

    @Test
    public void testIllegalUpdate() {
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(4, l, secureRandom);
            senderOutput.split(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(4, l, secureRandom);
            LnotReceiverOutput receiverOutput = LnotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(4, l, secureRandom);
            senderOutput.split(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(4, l, secureRandom);
            LnotReceiverOutput receiverOutput = LnotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(4, l, secureRandom);
            senderOutput.reduce(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(4, l, secureRandom);
            LnotReceiverOutput receiverOutput = LnotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(4, l, secureRandom);
            senderOutput.reduce(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(4, l, secureRandom);
            LnotReceiverOutput receiverOutput = LnotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.reduce(5);
        });
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(num, l, secureRandom);
        LnotReceiverOutput receiverOutput = LnotReceiverOutput.createRandom(senderOutput, secureRandom);
        OtTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    private void testReduce(int num) {
        // reduce 1
        LnotSenderOutput senderOutput1 = LnotSenderOutput.createRandom(num, l, secureRandom);
        LnotReceiverOutput receiverOutput1 = LnotReceiverOutput.createRandom(senderOutput1, secureRandom);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        OtTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // reduce all
        LnotSenderOutput senderOutputAll = LnotSenderOutput.createRandom(num, l, secureRandom);
        LnotReceiverOutput receiverOutputAll = LnotReceiverOutput.createRandom(senderOutputAll, secureRandom);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        OtTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            LnotSenderOutput senderOutputNum = LnotSenderOutput.createRandom(num, l, secureRandom);
            LnotReceiverOutput receiverOutputNum = LnotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            OtTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // reduce half
            LnotSenderOutput senderOutputHalf = LnotSenderOutput.createRandom(num, l, secureRandom);
            LnotReceiverOutput receiverOutputHalf = LnotReceiverOutput.createRandom(senderOutputHalf, secureRandom);
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
        LnotSenderOutput senderOutput = LnotSenderOutput.createRandom(num1, l, secureRandom);
        LnotSenderOutput mergeSenderOutput = LnotSenderOutput.createRandom(num2, l, secureRandom);
        LnotReceiverOutput receiverOutput = LnotReceiverOutput.createRandom(senderOutput, secureRandom);
        LnotReceiverOutput mergeReceiverOutput = LnotReceiverOutput.createRandom(mergeSenderOutput, secureRandom);
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
        // split 1
        LnotSenderOutput senderOutput1 = LnotSenderOutput.createRandom(num, l, secureRandom);
        LnotReceiverOutput receiverOutput1 = LnotReceiverOutput.createRandom(senderOutput1, secureRandom);
        LnotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        LnotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        OtTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        OtTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        LnotSenderOutput senderOutputAll = LnotSenderOutput.createRandom(num, l, secureRandom);
        LnotReceiverOutput receiverOutputAll = LnotReceiverOutput.createRandom(senderOutputAll, secureRandom);
        LnotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        LnotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        OtTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        OtTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            LnotSenderOutput senderOutputNum = LnotSenderOutput.createRandom(num, l, secureRandom);
            LnotReceiverOutput receiverOutputNum = LnotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            LnotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            LnotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            OtTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            OtTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputNum);
            // split half
            LnotSenderOutput senderOutputHalf = LnotSenderOutput.createRandom(num, l, secureRandom);
            LnotReceiverOutput receiverOutputHalf = LnotReceiverOutput.createRandom(senderOutputHalf, secureRandom);
            LnotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            LnotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
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
        // split and merge 1
        LnotSenderOutput senderOutput1 = LnotSenderOutput.createRandom(num, l, secureRandom);
        LnotSenderOutput copySenderOutput1 = senderOutput1.copy();
        LnotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        senderOutput1.merge(splitSenderOutput1);
        Assert.assertEquals(copySenderOutput1, senderOutput1);
        LnotReceiverOutput receiverOutput1 = LnotReceiverOutput.createRandom(senderOutput1, secureRandom);
        LnotReceiverOutput copyReceiverOutput1 = receiverOutput1.copy();
        LnotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        receiverOutput1.merge(splitReceiverOutput1);
        Assert.assertEquals(copyReceiverOutput1, receiverOutput1);
        // split and merge all
        LnotSenderOutput senderOutputAll = LnotSenderOutput.createRandom(num, l, secureRandom);
        LnotSenderOutput copySenderOutputAll = senderOutputAll.copy();
        LnotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        senderOutputAll.merge(splitSenderOutputAll);
        Assert.assertEquals(copySenderOutputAll, senderOutputAll);
        LnotReceiverOutput receiverOutputAll = LnotReceiverOutput.createRandom(senderOutputAll, secureRandom);
        LnotReceiverOutput copyReceiverOutputAll = receiverOutputAll.copy();
        LnotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        receiverOutputAll.merge(splitReceiverOutputAll);
        Assert.assertEquals(copyReceiverOutputAll, receiverOutputAll);
        if (num > 1) {
            // split and merge num - 1
            LnotSenderOutput senderOutputNum = LnotSenderOutput.createRandom(num, l, secureRandom);
            LnotSenderOutput copySenderOutputNum = senderOutputNum.copy();
            LnotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            senderOutputNum.merge(splitSenderOutputNum);
            Assert.assertEquals(copySenderOutputNum, senderOutputNum);
            LnotReceiverOutput receiverOutputNum = LnotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            LnotReceiverOutput copyReceiverOutputNum = receiverOutputNum.copy();
            LnotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            receiverOutputNum.merge(splitReceiverOutputNum);
            Assert.assertEquals(copyReceiverOutputNum, receiverOutputNum);
            // split half
            LnotSenderOutput senderOutputHalf = LnotSenderOutput.createRandom(num, l, secureRandom);
            LnotSenderOutput copySenderOutputHalf = senderOutputHalf.copy();
            LnotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            senderOutputHalf.merge(splitSenderOutputHalf);
            Assert.assertEquals(copySenderOutputHalf, senderOutputHalf);
            LnotReceiverOutput receiverOutputHalf = LnotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            LnotReceiverOutput copyReceiverOutputHalf = receiverOutputHalf.copy();
            LnotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            receiverOutputHalf.merge(splitReceiverOutputHalf);
            Assert.assertEquals(copyReceiverOutputHalf, receiverOutputHalf);
        }
    }
}
