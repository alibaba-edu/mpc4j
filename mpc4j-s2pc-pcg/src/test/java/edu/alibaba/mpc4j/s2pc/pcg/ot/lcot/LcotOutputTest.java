package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtTestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 1-out-of-2^l COT output test.
 *
 * @author Weiran Liu
 * @date 2024/7/2
 */
@RunWith(Parameterized.class)
public class LcotOutputTest {
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

        for (int l : new int[] {1, 3, 8, 64}) {
            configurations.add(new Object[] {"l = " + l, l});
        }

        return configurations;
    }

    /**
     * input bit length
     */
    private final int inputBitLength;
    /**
     * input byte length
     */
    private final int inputByteLength;
    /**
     * output bit length
     */
    private final int outputBitLength;
    /**
     * output byte length
     */
    private final int outputByteLength;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;

    public LcotOutputTest(String name, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.inputBitLength = l;
        inputByteLength = CommonUtils.getByteLength(l);
        LinearCoder linearCoder = LinearCoderFactory.getInstance(l);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalSenderInputs() {
        // create a sender output with short length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(outputByteLength - 1, secureRandom);
            byte[][] r0Array = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength, outputBitLength, secureRandom);
            LcotSenderOutput.create(inputBitLength, delta, r0Array);
        });
        // create a sender output with long length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(outputByteLength + 1, secureRandom);
            byte[][] r0Array = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength, outputBitLength, secureRandom);
            LcotSenderOutput.create(inputBitLength, delta, r0Array);
        });
        // create a sender output with short length qs
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(outputByteLength, secureRandom);
            byte[][] qsArray = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength - 1, secureRandom);
            LcotSenderOutput.create(inputBitLength, delta, qsArray);
        });
        // create a sender output with long length qs
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(outputByteLength, secureRandom);
            byte[][] qsArray = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength + 1, secureRandom);
            LcotSenderOutput.create(inputBitLength, delta, qsArray);
        });
        // merge two sender outputs with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta0 = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
            byte[][] qsArray0 = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength, outputBitLength, secureRandom);
            LcotSenderOutput senderOutput0 = LcotSenderOutput.create(inputBitLength, delta0, qsArray0);
            byte[] delta1 = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
            byte[][] r0Array1 = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength, outputBitLength, secureRandom);
            LcotSenderOutput senderOutput1 = LcotSenderOutput.create(inputBitLength, delta1, r0Array1);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testIllegalReceiverInputs() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength, outputBitLength, secureRandom);
            LcotReceiverOutput.create(inputBitLength, new byte[0][], rbArray);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] choices = BytesUtils.randomByteArrayVector(MAX_NUM, inputByteLength, inputBitLength, secureRandom);
            LcotReceiverOutput.create(inputBitLength, choices, new byte[0][]);
        });
        // create a receiver output with mismatched num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] choices = BytesUtils.randomByteArrayVector(MIN_NUM, inputByteLength, inputBitLength, secureRandom);
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength, outputBitLength, secureRandom);
            LcotReceiverOutput.create(inputBitLength, choices, rbArray);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] choices = BytesUtils.randomByteArrayVector(MAX_NUM, inputByteLength, inputBitLength, secureRandom);
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MIN_NUM, outputByteLength, outputBitLength, secureRandom);
            LcotReceiverOutput.create(inputBitLength, choices, rbArray);
        });
        // create a receiver output with short length choices
        if (inputByteLength > 1) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] choices = BytesUtils.randomByteArrayVector(MAX_NUM, inputByteLength - 1, secureRandom);
                byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength, outputBitLength, secureRandom);
                LcotReceiverOutput.create(inputBitLength, choices, rbArray);
            });
        }
        // create a receiver output with long length choices
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] choices = BytesUtils.randomByteArrayVector(MAX_NUM, inputByteLength + 1, secureRandom);
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength, outputBitLength, secureRandom);
            LcotReceiverOutput.create(inputBitLength, choices, rbArray);
        });
        // create a receiver output with short length rb
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] choices = BytesUtils.randomByteArrayVector(MAX_NUM, inputByteLength, inputBitLength, secureRandom);
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength - 1, secureRandom);
            LcotReceiverOutput.create(inputBitLength, choices, rbArray);
        });
        // create a receiver output with long length rb
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] choices = BytesUtils.randomByteArrayVector(MAX_NUM, inputByteLength, inputBitLength, secureRandom);
            byte[][] rbArray = BytesUtils.randomByteArrayVector(MAX_NUM, outputByteLength + 1, secureRandom);
            LcotReceiverOutput.create(inputBitLength, choices, rbArray);
        });
    }

    @Test
    public void testIllegalUpdate() {
        byte[] delta = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(4, inputBitLength, delta, secureRandom);
            senderOutput.split(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(4, inputBitLength, delta, secureRandom);
            LcotReceiverOutput receiverOutput = LcotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(4, inputBitLength, delta, secureRandom);
            senderOutput.split(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(4, inputBitLength, delta, secureRandom);
            LcotReceiverOutput receiverOutput = LcotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(4, inputBitLength, delta, secureRandom);
            senderOutput.reduce(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(4, inputBitLength, delta, secureRandom);
            LcotReceiverOutput receiverOutput = LcotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(4, inputBitLength, delta, secureRandom);
            senderOutput.reduce(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(4, inputBitLength, delta, secureRandom);
            LcotReceiverOutput receiverOutput = LcotReceiverOutput.createRandom(senderOutput, secureRandom);
            receiverOutput.reduce(5);
        });
        // merge two sender outputs with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta0 = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
            LcotSenderOutput senderOutput0 = LcotSenderOutput.createRandom(4, inputBitLength, delta0, secureRandom);
            byte[] delta1 = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
            LcotSenderOutput senderOutput1 = LcotSenderOutput.createRandom(4, inputBitLength, delta1, secureRandom);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        byte[] delta = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
        LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
        LcotReceiverOutput receiverOutput = LcotReceiverOutput.createRandom(senderOutput, secureRandom);
        OtTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        byte[] delta = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
        // reduce 1
        LcotSenderOutput senderOutput1 = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
        LcotReceiverOutput receiverOutput1 = LcotReceiverOutput.createRandom(senderOutput1, secureRandom);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        OtTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // reduce all
        LcotSenderOutput senderOutputAll = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
        LcotReceiverOutput receiverOutputAll = LcotReceiverOutput.createRandom(senderOutputAll, secureRandom);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        OtTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            LcotSenderOutput senderOutputNum = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
            LcotReceiverOutput receiverOutputNum = LcotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            OtTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // reduce half
            LcotSenderOutput senderOutputHalf = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
            LcotReceiverOutput receiverOutputHalf = LcotReceiverOutput.createRandom(senderOutputHalf, secureRandom);
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
        byte[] delta = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
        LcotSenderOutput senderOutput = LcotSenderOutput.createRandom(num1, inputBitLength, delta, secureRandom);
        LcotSenderOutput mergeSenderOutput = LcotSenderOutput.createRandom(num2, inputBitLength, delta, secureRandom);
        LcotReceiverOutput receiverOutput = LcotReceiverOutput.createRandom(senderOutput, secureRandom);
        LcotReceiverOutput mergeReceiverOutput = LcotReceiverOutput.createRandom(mergeSenderOutput, secureRandom);
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
        byte[] delta = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
        // split 1
        LcotSenderOutput senderOutput1 = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
        LcotReceiverOutput receiverOutput1 = LcotReceiverOutput.createRandom(senderOutput1, secureRandom);
        LcotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        LcotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        OtTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        OtTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        LcotSenderOutput senderOutputAll = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
        LcotReceiverOutput receiverOutputAll = LcotReceiverOutput.createRandom(senderOutputAll, secureRandom);
        LcotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        LcotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        OtTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        OtTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            LcotSenderOutput senderOutputNum = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
            LcotReceiverOutput receiverOutputNum = LcotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            LcotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            LcotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            OtTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            OtTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputNum);
            // split half
            LcotSenderOutput senderOutputHalf = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
            LcotReceiverOutput receiverOutputHalf = LcotReceiverOutput.createRandom(senderOutputHalf, secureRandom);
            LcotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            LcotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
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
        byte[] delta = BytesUtils.randomByteArray(outputByteLength, outputBitLength, secureRandom);
        // split and merge 1
        LcotSenderOutput senderOutput1 = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
        LcotSenderOutput copySenderOutput1 = senderOutput1.copy();
        LcotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        senderOutput1.merge(splitSenderOutput1);
        Assert.assertEquals(copySenderOutput1, senderOutput1);
        LcotReceiverOutput receiverOutput1 = LcotReceiverOutput.createRandom(senderOutput1, secureRandom);
        LcotReceiverOutput copyReceiverOutput1 = receiverOutput1.copy();
        LcotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        receiverOutput1.merge(splitReceiverOutput1);
        Assert.assertEquals(copyReceiverOutput1, receiverOutput1);
        // split and merge all
        LcotSenderOutput senderOutputAll = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
        LcotSenderOutput copySenderOutputAll = senderOutputAll.copy();
        LcotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        senderOutputAll.merge(splitSenderOutputAll);
        Assert.assertEquals(copySenderOutputAll, senderOutputAll);
        LcotReceiverOutput receiverOutputAll = LcotReceiverOutput.createRandom(senderOutputAll, secureRandom);
        LcotReceiverOutput copyReceiverOutputAll = receiverOutputAll.copy();
        LcotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        receiverOutputAll.merge(splitReceiverOutputAll);
        Assert.assertEquals(copyReceiverOutputAll, receiverOutputAll);
        if (num > 1) {
            // split and merge num - 1
            LcotSenderOutput senderOutputNum = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
            LcotSenderOutput copySenderOutputNum = senderOutputNum.copy();
            LcotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            senderOutputNum.merge(splitSenderOutputNum);
            Assert.assertEquals(copySenderOutputNum, senderOutputNum);
            LcotReceiverOutput receiverOutputNum = LcotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            LcotReceiverOutput copyReceiverOutputNum = receiverOutputNum.copy();
            LcotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            receiverOutputNum.merge(splitReceiverOutputNum);
            Assert.assertEquals(copyReceiverOutputNum, receiverOutputNum);
            // split half
            LcotSenderOutput senderOutputHalf = LcotSenderOutput.createRandom(num, inputBitLength, delta, secureRandom);
            LcotSenderOutput copySenderOutputHalf = senderOutputHalf.copy();
            LcotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            senderOutputHalf.merge(splitSenderOutputHalf);
            Assert.assertEquals(copySenderOutputHalf, senderOutputHalf);
            LcotReceiverOutput receiverOutputHalf = LcotReceiverOutput.createRandom(senderOutputNum, secureRandom);
            LcotReceiverOutput copyReceiverOutputHalf = receiverOutputHalf.copy();
            LcotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            receiverOutputHalf.merge(splitReceiverOutputHalf);
            Assert.assertEquals(copyReceiverOutputHalf, receiverOutputHalf);
        }
    }
}
