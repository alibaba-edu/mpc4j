package edu.alibaba.mpc4j.s2pc.pcg.vole.z2;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Z2-VOLE输出测试。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public class Z2VoleOutputTest {
    /**
     * 最小数量
     */
    private static final int MIN_NUM = 1;
    /**
     * 最大数量
     */
    private static final int MAX_NUM = 127;
    /**
     * 最大字节数量
     */
    private static final int MAX_BYTE_NUM = CommonUtils.getByteLength(MAX_NUM);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testIllegalInputs() {
        try {
            // 创建长度为0的接收方输出
            Z2VoleReceiverOutput.create(0, true, new byte[0]);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with num = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建q长度不正确的接收方输出
            byte[] q = new byte[MAX_BYTE_NUM];
            Arrays.fill(q, (byte)0xFF);
            Z2VoleReceiverOutput.create(MAX_NUM, true, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with wrong length q");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建q长度较大的接收方输出
            byte[] q = new byte[MAX_BYTE_NUM + 1];
            Z2VoleReceiverOutput.create(MAX_NUM, true, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with large length q");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建q长度较小的接收方输出
            byte[] q = new byte[MAX_BYTE_NUM - 1];
            Z2VoleReceiverOutput.create(MAX_NUM, true, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with small length q");
        } catch (AssertionError ignored) {

        }
        try {
            // 合并两个Δ不相等的接收方输出
            byte[] q = new byte[MAX_BYTE_NUM];
            SECURE_RANDOM.nextBytes(q);
            BytesUtils.reduceByteArray(q, MAX_NUM);
            Z2VoleReceiverOutput receiverOutput0 = Z2VoleReceiverOutput.create(MAX_NUM, true, q);
            Z2VoleReceiverOutput receiverOutput1 = Z2VoleReceiverOutput.create(MAX_NUM, false, q);
            receiverOutput0.merge(receiverOutput1);
            throw new IllegalStateException("ERROR: successfully merge ReceiverOutput with different Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建长度为0的发送方输出
            Z2VoleSenderOutput.create(0, new byte[0], new byte[0]);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x长度不正确的发送方输出
            byte[] x = new byte[MAX_BYTE_NUM];
            Arrays.fill(x, (byte)0xFF);
            byte[] t = new byte[MAX_BYTE_NUM];
            SECURE_RANDOM.nextBytes(t);
            BytesUtils.reduceByteArray(t, MAX_NUM);
            Z2VoleSenderOutput.create(MAX_NUM, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with wrong length x");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x长度较大的发送方输出
            byte[] x = new byte[MAX_BYTE_NUM + 1];
            byte[] t = new byte[MAX_BYTE_NUM];
            SECURE_RANDOM.nextBytes(t);
            BytesUtils.reduceByteArray(t, MAX_NUM);
            Z2VoleSenderOutput.create(MAX_NUM, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with large length x");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x长度较小的发送方输出
            byte[] x = new byte[MAX_BYTE_NUM - 1];
            byte[] t = new byte[MAX_BYTE_NUM];
            SECURE_RANDOM.nextBytes(t);
            BytesUtils.reduceByteArray(t, MAX_NUM);
            Z2VoleSenderOutput.create(MAX_NUM, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with small length x");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建t长度不正确的发送方输出
            byte[] x = new byte[MAX_BYTE_NUM];
            SECURE_RANDOM.nextBytes(x);
            BytesUtils.reduceByteArray(x, MAX_NUM);
            byte[] t = new byte[MAX_BYTE_NUM];
            Arrays.fill(t, (byte)0xFF);
            Z2VoleSenderOutput.create(MAX_NUM, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with wrong length t");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建t长度较大的接收方输出
            byte[] x = new byte[MAX_BYTE_NUM];
            SECURE_RANDOM.nextBytes(x);
            BytesUtils.reduceByteArray(x, MAX_NUM);
            byte[] t = new byte[MAX_BYTE_NUM + 1];
            Z2VoleSenderOutput.create(MAX_NUM, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with large length t");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建t长度较小的接收方输出
            byte[] x = new byte[MAX_BYTE_NUM];
            SECURE_RANDOM.nextBytes(x);
            BytesUtils.reduceByteArray(x, MAX_NUM);
            byte[] t = new byte[MAX_BYTE_NUM - 1];
            Z2VoleSenderOutput.create(MAX_NUM, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with small length t");
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
        boolean delta = SECURE_RANDOM.nextBoolean();
        // 减小到1
        Z2VoleReceiverOutput receiverOutput1 = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Z2VoleSenderOutput senderOutput1 = Z2VoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        Z2VoleTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // 减小到相同长度
        Z2VoleReceiverOutput receiverOutputAll = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Z2VoleSenderOutput senderOutputAll = Z2VoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        Z2VoleTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // 减小num - 1
            Z2VoleReceiverOutput receiverOutputNum = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
            Z2VoleSenderOutput senderOutputNum = Z2VoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            Z2VoleTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // 减小到一半
            Z2VoleReceiverOutput receiverOutputHalf = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
            Z2VoleSenderOutput senderOutputHalf = Z2VoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            Z2VoleTestUtils.assertOutput(num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        boolean delta = SECURE_RANDOM.nextBoolean();
        Z2VoleSenderOutput senderOutput = Z2VoleSenderOutput.createEmpty();
        Z2VoleSenderOutput mergeSenderOutput = Z2VoleSenderOutput.createEmpty();
        Z2VoleReceiverOutput receiverOutput = Z2VoleReceiverOutput.createEmpty(delta);
        Z2VoleReceiverOutput mergeReceiverOutput = Z2VoleReceiverOutput.createEmpty(delta);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Z2VoleTestUtils.assertOutput(0, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        boolean delta = SECURE_RANDOM.nextBoolean();
        Z2VoleReceiverOutput receiverOutput = Z2VoleReceiverOutput.createEmpty(delta);
        Z2VoleReceiverOutput mergeReceiverOutput = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Z2VoleSenderOutput senderOutput = Z2VoleSenderOutput.createEmpty();
        Z2VoleSenderOutput mergeSenderOutput = Z2VoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Z2VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        boolean delta = SECURE_RANDOM.nextBoolean();
        Z2VoleReceiverOutput receiverOutput = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Z2VoleReceiverOutput mergeReceiverOutput = Z2VoleReceiverOutput.createEmpty(delta);
        Z2VoleSenderOutput senderOutput = Z2VoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        Z2VoleSenderOutput mergeSenderOutput = Z2VoleSenderOutput.createEmpty();
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Z2VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        boolean delta = SECURE_RANDOM.nextBoolean();
        Z2VoleReceiverOutput receiverOutput = Z2VoleTestUtils.genReceiverOutput(num1, delta, SECURE_RANDOM);
        Z2VoleReceiverOutput mergeReceiverOutput = Z2VoleTestUtils.genReceiverOutput(num2, delta, SECURE_RANDOM);
        Z2VoleSenderOutput senderOutput = Z2VoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        Z2VoleSenderOutput mergeSenderOutput = Z2VoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Z2VoleTestUtils.assertOutput(num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        boolean delta = SECURE_RANDOM.nextBoolean();
        // 切分1比特
        Z2VoleReceiverOutput receiverOutput1 = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Z2VoleSenderOutput senderOutput1 = Z2VoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        Z2VoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        Z2VoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        Z2VoleTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        Z2VoleTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // 切分全部比特
        Z2VoleReceiverOutput receiverOutputAll = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Z2VoleSenderOutput senderOutputAll = Z2VoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        Z2VoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        Z2VoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        Z2VoleTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        Z2VoleTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // 切分n - 1比特
            Z2VoleReceiverOutput receiverOutputNum = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
            Z2VoleSenderOutput senderOutputNum = Z2VoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            Z2VoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            Z2VoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            Z2VoleTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            Z2VoleTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // 切分一半比特
            Z2VoleReceiverOutput receiverOutputHalf = Z2VoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
            Z2VoleSenderOutput senderOutputHalf = Z2VoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            Z2VoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            Z2VoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            Z2VoleTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            Z2VoleTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
