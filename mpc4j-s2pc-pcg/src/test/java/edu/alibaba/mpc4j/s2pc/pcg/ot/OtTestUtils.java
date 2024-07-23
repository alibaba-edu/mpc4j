package edu.alibaba.mpc4j.s2pc.pcg.ot;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;
import org.junit.Assert;

/**
 * COT test utilities.
 *
 * @author Weiran Liu
 * @date 2022/01/25
 */
public class OtTestUtils {
    /**
     * private constructor.
     */
    private OtTestUtils() {
        // empty
    }

    /**
     * Asserts the output.
     *
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(int num, CotSenderOutput senderOutput, CotReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        if (num == 0) {
            Assert.assertArrayEquals(new byte[0][], senderOutput.getR0Array());
            Assert.assertArrayEquals(new boolean[0], receiverOutput.getChoices());
            Assert.assertArrayEquals(new byte[0][], receiverOutput.getRbArray());
        } else {
            IntStream.range(0, num).parallel().forEach(index -> {
                ByteBuffer rb = ByteBuffer.wrap(receiverOutput.getRb(index));
                ByteBuffer r0 = ByteBuffer.wrap(senderOutput.getR0(index));
                ByteBuffer r1 = ByteBuffer.wrap(senderOutput.getR1(index));
                boolean choice = receiverOutput.getChoice(index);
                if (choice) {
                    Assert.assertEquals(rb, r1);
                    Assert.assertNotEquals(rb, r0);
                } else {
                    Assert.assertEquals(rb, r0);
                    Assert.assertNotEquals(rb, r1);
                }
            });
        }
    }

    /**
     * Asserts the output.
     *
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(int num, LcotSenderOutput senderOutput, LcotReceiverOutput receiverOutput) {
        // verify input length
        Assert.assertEquals(senderOutput.getInputBitLength(), receiverOutput.getInputBitLength());
        Assert.assertEquals(senderOutput.getInputByteLength(), receiverOutput.getInputByteLength());
        // verify output length
        Assert.assertEquals(senderOutput.getOutputBitLength(), receiverOutput.getOutputBitLength());
        Assert.assertEquals(senderOutput.getOutputByteLength(), receiverOutput.getOutputByteLength());
        // verify num
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        if (num == 0) {
            Assert.assertArrayEquals(new byte[0][], senderOutput.getQsArray());
            Assert.assertArrayEquals(new byte[0][], receiverOutput.getChoices());
            Assert.assertArrayEquals(new byte[0][], receiverOutput.getRbArray());
        } else {
            // verify correlation
            IntStream.range(0, num).forEach(index -> {
                byte[] choice = receiverOutput.getChoice(index);
                Assert.assertArrayEquals(receiverOutput.getRb(index), senderOutput.getRb(index, choice));
            });
            // verify homomorphism, reduce verify number using sqrt
            LinearCoder linearCoder = senderOutput.getLinearCoder();
            byte[][] choices = receiverOutput.getChoices();
            for (int i = 0; i < num; i += (int) Math.sqrt(num)) {
                for (int j = i + 1; j < num; j += (int) Math.sqrt(num)) {
                    byte[] tij = BytesUtils.xor(receiverOutput.getRb(i), receiverOutput.getRb(j));
                    byte[] qij = BytesUtils.xor(senderOutput.getQ(i), senderOutput.getQ(j));
                    byte[] choicei = BytesUtils.paddingByteArray(choices[i], linearCoder.getDatawordByteLength());
                    byte[] choicej = BytesUtils.paddingByteArray(choices[j], linearCoder.getDatawordByteLength());
                    byte[] choiceij = BytesUtils.xor(choicei, choicej);
                    BytesUtils.xori(qij, BytesUtils.and(senderOutput.getDelta(), linearCoder.encode(choiceij)));
                    Assert.assertArrayEquals(tij, qij);
                }
            }
        }
    }

    /**
     * Asserts the output.
     *
     * @param num            num.
     * @param senderOutput   sender output.
     * @param receiverOutput receiver output.
     */
    public static void assertOutput(int num, LnotSenderOutput senderOutput, LnotReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getL(), receiverOutput.getL());
        Assert.assertEquals(senderOutput.getN(), receiverOutput.getN());
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        if (num != 0) {
            int n = senderOutput.getN();
            IntStream.range(0, num).forEach(i -> {
                int correctChoice = receiverOutput.getChoice(i);
                ByteBuffer rb = ByteBuffer.wrap(receiverOutput.getRb(i));
                for (int choice = 0; choice < n; choice++) {
                    ByteBuffer ri = ByteBuffer.wrap(senderOutput.getRb(i, choice));
                    if (choice == correctChoice) {
                        Assert.assertEquals(rb, ri);
                    } else {
                        Assert.assertNotEquals(rb, ri);
                    }
                }
            });
        }
    }
}
