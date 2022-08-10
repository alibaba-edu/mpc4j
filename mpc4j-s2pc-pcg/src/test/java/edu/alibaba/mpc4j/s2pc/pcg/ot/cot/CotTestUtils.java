package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import java.security.SecureRandom;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;

/**
 * COT测试工具。
 *
 * @author Weiran Liu
 * @date 2022/01/25
 */
public class CotTestUtils {
    /**
     * 私有构造函数
     */
    private CotTestUtils() {
        // empty
    }

    /**
     * 生成发送方输出。
     *
     * @param num          数量。
     * @param delta        关联值Δ。
     * @param secureRandom 随机状态。
     * @return 发送方输出。
     */
    public static CotSenderOutput genSenderOutput(int num, byte[] delta, SecureRandom secureRandom) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert num >= 0 : "num must be greater than or equal to 0";
        if (num == 0) {
            return CotSenderOutput.createEmpty(delta);
        }
        byte[][] r0Array = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(r0);
                return r0;
            })
            .toArray(byte[][]::new);
        return CotSenderOutput.create(delta, r0Array);
    }

    /**
     * 生成接收方输出。
     *
     * @param senderOutput 发送方输出。
     * @param secureRandom 随机状态。
     * @return 接收方输出。
     */
    public static CotReceiverOutput genReceiverOutput(CotSenderOutput senderOutput, SecureRandom secureRandom) {
        int num = senderOutput.getNum();
        if (num == 0) {
            return CotReceiverOutput.createEmpty();
        }
        boolean[] choices = new boolean[num];
        IntStream.range(0, num).forEach(index -> choices[index] = secureRandom.nextBoolean());
        byte[][] rbArray = IntStream.range(0, num)
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
     * 验证输出结果。
     *
     * @param num            数量。
     * @param senderOutput   发送方输出。
     * @param receiverOutput 接收方输出。
     */
    public static void assertOutput(int num, CotSenderOutput senderOutput, CotReceiverOutput receiverOutput) {
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, senderOutput.getR0Array().length);
            Assert.assertEquals(0, senderOutput.getR1Array().length);
            Assert.assertEquals(0, receiverOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getRbArray().length);
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            IntStream.range(0, num).forEach(index ->
                Assert.assertArrayEquals(
                    receiverOutput.getRb(index),
                    receiverOutput.getChoice(index) ? senderOutput.getR1(index) : senderOutput.getR0(index)
                ));
        }

    }
}
