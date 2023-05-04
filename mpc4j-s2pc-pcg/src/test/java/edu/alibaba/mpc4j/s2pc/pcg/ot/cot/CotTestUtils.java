package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;

/**
 * COT test utilities.
 *
 * @author Weiran Liu
 * @date 2022/01/25
 */
public class CotTestUtils {
    /**
     * private constructor.
     */
    private CotTestUtils() {
        // empty
    }

    /**
     * Generates a sender output.
     *
     * @param num          num.
     * @param delta        Δ.
     * @param secureRandom the random state.
     * @return a sender output.
     */
    public static CotSenderOutput genSenderOutput(int num, byte[] delta, SecureRandom secureRandom) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert num >= 0 : "num must be greater than or equal to 0: " + num;
        if (num == 0) {
            return CotSenderOutput.createEmpty(delta);
        }
        byte[][] r0Array = IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> {
                byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(r0);
                return r0;
            })
            .toArray(byte[][]::new);
        return CotSenderOutput.create(delta, r0Array);
    }

    /**
     * Generates a receiver output.
     *
     * @param senderOutput the sender output.
     * @param secureRandom the random state.
     * @return a receiver output.
     */
    public static CotReceiverOutput genReceiverOutput(CotSenderOutput senderOutput, SecureRandom secureRandom) {
        int num = senderOutput.getNum();
        if (num == 0) {
            return CotReceiverOutput.createEmpty();
        }
        boolean[] choices = new boolean[num];
        IntStream.range(0, num).forEach(index -> choices[index] = secureRandom.nextBoolean());
        byte[][] rbArray = IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> {
                if (choices[index]) {
                    return BytesUtils.clone(senderOutput.getR1(index));
                } else {
                    return BytesUtils.clone(senderOutput.getR0(index));
                }
            })
            .toArray(byte[][]::new);
        return CotReceiverOutput.create(choices, rbArray);
    }

    /**
     * asserts the output.
     *
     * @param num            num.
     * @param senderOutput   the sender output.
     * @param receiverOutput the receiver output.
     */
    public static void assertOutput(int num, CotSenderOutput senderOutput, CotReceiverOutput receiverOutput) {
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getNum());
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
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
}
