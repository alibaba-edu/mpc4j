package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * COT输出测试。
 *
 * @author Weiran Liu
 * @date 2022/4/11
 */
public class CotOutputTest {
    /**
     * 最小数量
     */
    private static final int MIN_NUM = 1;
    /**
     * 最大数量
     */
    private static final int MAX_NUM = 128;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认全0关联值Δ
     */
    private static final byte[] ALL_ZERO_DELTA = new byte[]{
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };
    /**
     * 默认全1关联值Δ
     */
    private static final byte[] ALL_ONE_DELTA = new byte[]{
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
    };

    @Test
    public void testIllegalInputs() {
        try {
            // 创建长度为0的发送方输出
            CotSenderOutput.create(ALL_ONE_DELTA, new byte[0][]);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ长度过小的发送方输出
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] r0Array = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> IntUtils.nonNegIntToFixedByteArray(index, CommonConstants.BLOCK_BYTE_LENGTH))
                .toArray(byte[][]::new);
            CotSenderOutput.create(delta, r0Array);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with small length Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ长度过大的发送方输出
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] r0Array = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> IntUtils.nonNegIntToFixedByteArray(index, CommonConstants.BLOCK_BYTE_LENGTH))
                .toArray(byte[][]::new);
            CotSenderOutput.create(delta, r0Array);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with large length Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建R0长度过小的发送方输出
            byte[][] r0Array = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
                    SECURE_RANDOM.nextBytes(r0);
                    return r0;
                })
                .toArray(byte[][]::new);
            CotSenderOutput.create(ALL_ONE_DELTA, r0Array);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with small length R0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建R0长度过大的发送方输出
            byte[][] r0Array = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] r0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1];
                    SECURE_RANDOM.nextBytes(r0);
                    return r0;
                })
                .toArray(byte[][]::new);
            CotSenderOutput.create(ALL_ONE_DELTA, r0Array);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with large length R0");
        } catch (AssertionError ignored) {

        }
        try {
            // 合并两个Δ不相等的发送方输出
            byte[][] r0Array = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> IntUtils.nonNegIntToFixedByteArray(index, CommonConstants.BLOCK_BYTE_LENGTH))
                .toArray(byte[][]::new);
            CotSenderOutput senderOutput0 = CotSenderOutput.create(ALL_ONE_DELTA, r0Array);
            CotSenderOutput senderOutput1 = CotSenderOutput.create(ALL_ZERO_DELTA, r0Array);
            senderOutput0.merge(senderOutput1);
            throw new IllegalStateException("ERROR: successfully merge SenderOutput with different Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建长度为0的接收方输出
            CotReceiverOutput.create(new boolean[0], new byte[0][]);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建选择比特和Rb长度不匹配的接收方输出
            boolean[] choices = new boolean[MIN_NUM];
            IntStream.range(0, choices.length).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            CotReceiverOutput.create(choices, rbArray);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with different array length");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Rb长度过小的接收方输出
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
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with small length Rb");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Rb长度过大的接收方输出
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
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with large length Rb");
        } catch (AssertionError ignored) {

        }
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
        // 减小到1
        CotSenderOutput senderOutput1 = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutput1 = CotTestUtils.genReceiverOutput(senderOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        CotTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // 减小到相同长度
        CotSenderOutput senderOutputAll = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutputAll = CotTestUtils.genReceiverOutput(senderOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        CotTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // 减小n - 1
            CotSenderOutput senderOutputNum = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
            CotReceiverOutput receiverOutputNum = CotTestUtils.genReceiverOutput(senderOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            CotTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // 减小到一半
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
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
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
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
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
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
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
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
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
        // 切分1比特
        CotSenderOutput senderOutput1 = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutput1 = CotTestUtils.genReceiverOutput(senderOutput1, SECURE_RANDOM);
        CotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        CotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        CotTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        CotTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // 切分全部比特
        CotSenderOutput senderOutputAll = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
        CotReceiverOutput receiverOutputAll = CotTestUtils.genReceiverOutput(senderOutputAll, SECURE_RANDOM);
        CotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        CotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        CotTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        CotTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // 切分n - 1比特
            CotSenderOutput senderOutputNum = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
            CotReceiverOutput receiverOutputNum = CotTestUtils.genReceiverOutput(senderOutputNum, SECURE_RANDOM);
            CotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            CotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            CotTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            CotTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputNum);
            // 切分一半比特
            CotSenderOutput senderOutputHalf = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
            CotReceiverOutput receiverOutputHalf = CotTestUtils.genReceiverOutput(senderOutputHalf, SECURE_RANDOM);
            CotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            CotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            CotTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            CotTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
