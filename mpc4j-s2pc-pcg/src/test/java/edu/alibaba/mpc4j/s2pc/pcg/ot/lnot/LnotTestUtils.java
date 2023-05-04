package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * 1-out-of-n OT test utilities.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class LnotTestUtils {
    /**
     * private constructor.
     */
    private LnotTestUtils() {
        // empty
    }

    /**
     * Generates a sender output.
     *
     * @param l            the choice bit length.
     * @param num          the num.
     * @param secureRandom the random state.
     * @return a sender output.
     */
    public static LnotSenderOutput genSenderOutput(int l, int num, SecureRandom secureRandom) {
        assert num >= 0 : "num must be greater than or equal to 0: " + num;
        if (num == 0) {
            return LnotSenderOutput.createEmpty(l);
        }
        int n = 1 << l;
        byte[][][] rsArray = IntStream.range(0, num)
            .parallel()
            .mapToObj(index ->
                IntStream.range(0, n)
                    .mapToObj(choice -> {
                        byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        secureRandom.nextBytes(ri);
                        return ri;
                    })
                    .toArray(byte[][]::new))
            .toArray(byte[][][]::new);
        return LnotSenderOutput.create(l, rsArray);
    }

    /**
     * Generates a receiver output.
     *
     * @param senderOutput the sender output.
     * @param secureRandom the random state.
     * @return a receiver output.
     */
    public static LnotReceiverOutput genReceiverOutput(LnotSenderOutput senderOutput, SecureRandom secureRandom) {
        int l = senderOutput.getL();
        int n = senderOutput.getN();
        int num = senderOutput.getNum();
        if (num == 0) {
            return LnotReceiverOutput.createEmpty(l);
        }
        int[] choices = IntStream.range(0, num)
            .map(index -> secureRandom.nextInt(n))
            .toArray();
        byte[][] rbArray = IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> senderOutput.getRb(index, choices[index]))
            .toArray(byte[][]::new);
        return LnotReceiverOutput.create(l, choices, rbArray);
    }

    /**
     * asserts the output.
     *
     * @param l            the choice bit length.
     * @param num            the num.
     * @param senderOutput   the sender output.
     * @param receiverOutput the receiver output.
     */
    public static void assertOutput(int l, int num, LnotSenderOutput senderOutput, LnotReceiverOutput receiverOutput) {
        Assert.assertEquals(l, senderOutput.getL());
        Assert.assertEquals(l, receiverOutput.getL());
        int n = 1 << l;
        Assert.assertEquals(n, senderOutput.getN());
        Assert.assertEquals(n, receiverOutput.getN());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getNum());
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            IntStream.range(0, num).parallel().forEach(index -> {
                int correctChoice = receiverOutput.getChoice(index);
                ByteBuffer rb = ByteBuffer.wrap(receiverOutput.getRb(index));
                for (int choice = 0; choice < n; choice++) {
                    ByteBuffer ri = ByteBuffer.wrap(senderOutput.getRb(index, choice));
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
