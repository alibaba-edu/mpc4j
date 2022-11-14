package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * GF2K-VOLE输出测试。
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
public class Gf2kVoleOutputTest {
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
     * GF(2^128)计算工具
     */
    private static final Gf2k GF2K = Gf2kFactory.createInstance(EnvType.STANDARD);
    /**
     * GF(2^128)元素字节长度
     */
    private static final int GF2K_BYTE_LENGTH = GF2K.getByteL();
    /**
     * 默认0关联值Δ
     */
    private static final byte[] ZERO_DELTA = GF2K.createZero();
    /**
     * 默认1关联值Δ
     */
    private static final byte[] ONE_DELTA = GF2K.createOne();

    @Test
    public void testIllegalInputs() {
        try {
            // 创建长度为0的接收方输出
            Gf2kVoleReceiverOutput.create(ONE_DELTA, new byte[0][]);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with num = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ长度过小的接收方输出
            byte[] delta = new byte[GF2K_BYTE_LENGTH - 1];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] qi = new byte[GF2K_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(qi);
                    return qi;
                })
                .toArray(byte[][]::new);
            Gf2kVoleReceiverOutput.create(delta, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with small length Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ长度过大的接收方输出
            byte[] delta = new byte[GF2K_BYTE_LENGTH + 1];
            SECURE_RANDOM.nextBytes(delta);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] qi = new byte[GF2K_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(qi);
                    return qi;
                })
                .toArray(byte[][]::new);
            Gf2kVoleReceiverOutput.create(delta, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with large length Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建q长度过小的接收方输出
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] qi = new byte[GF2K_BYTE_LENGTH - 1];
                    SECURE_RANDOM.nextBytes(qi);
                    return qi;
                })
                .toArray(byte[][]::new);
            Gf2kVoleReceiverOutput.create(ONE_DELTA, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with small length q");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建q长度过大的发送方输出
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] qi = new byte[GF2K_BYTE_LENGTH + 1];
                    SECURE_RANDOM.nextBytes(qi);
                    return qi;
                })
                .toArray(byte[][]::new);
            Gf2kVoleReceiverOutput.create(ONE_DELTA, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with large length q");
        } catch (AssertionError ignored) {

        }
        try {
            // 合并两个Δ不相等的接收方输出
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] qi = new byte[GF2K_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(qi);
                    return qi;
                })
                .toArray(byte[][]::new);
            Gf2kVoleReceiverOutput receiverOutput0 = Gf2kVoleReceiverOutput.create(ONE_DELTA, q);
            Gf2kVoleReceiverOutput receiverOutput1 = Gf2kVoleReceiverOutput.create(ZERO_DELTA, q);
            receiverOutput0.merge(receiverOutput1);
            throw new IllegalStateException("ERROR: successfully merge ReceiverOutput with different Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建长度为0的发送方输出
            Gf2kVoleSenderOutput.create(new byte[0][], new byte[0][]);
            throw new IllegalStateException("ERROR: successfully create SenderOutputs with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x长度过小的发送方输出
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] xi = new byte[GF2K_BYTE_LENGTH - 1];
                    SECURE_RANDOM.nextBytes(xi);
                    return xi;
                })
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] ti = new byte[GF2K_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(ti);
                    return ti;
                })
                .toArray(byte[][]::new);
            Gf2kVoleSenderOutput.create(x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutputs with small length x");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x长度过大的发送方输出
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] xi = new byte[GF2K_BYTE_LENGTH + 1];
                    SECURE_RANDOM.nextBytes(xi);
                    return xi;
                })
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] ti = new byte[GF2K_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(ti);
                    return ti;
                })
                .toArray(byte[][]::new);
            Gf2kVoleSenderOutput.create(x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutputs with large length x");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建t长度过小的发送方输出
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] xi = new byte[GF2K_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(xi);
                    return xi;
                })
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] ti = new byte[GF2K_BYTE_LENGTH - 1];
                    SECURE_RANDOM.nextBytes(ti);
                    return ti;
                })
                .toArray(byte[][]::new);
            Gf2kVoleSenderOutput.create(x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutputs with small length t");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建t长度过大的发送方输出
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] xi = new byte[GF2K_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(xi);
                    return xi;
                })
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] ti = new byte[GF2K_BYTE_LENGTH + 1];
                    SECURE_RANDOM.nextBytes(ti);
                    return ti;
                })
                .toArray(byte[][]::new);
            Gf2kVoleSenderOutput.create(x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutputs with large length t");
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
        byte[] delta = new byte[GF2K_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        // 减小到1
        Gf2kVoleReceiverOutput receiverOutput1 = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Gf2kVoleSenderOutput senderOutput1 = Gf2kVoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        Gf2kVoleTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // 减小到相同长度
        Gf2kVoleReceiverOutput receiverOutputAll = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Gf2kVoleSenderOutput senderOutputAll = Gf2kVoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        Gf2kVoleTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // 减小num - 1
            Gf2kVoleReceiverOutput receiverOutputNum = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
            Gf2kVoleSenderOutput senderOutputNum = Gf2kVoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            Gf2kVoleTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // 减小到一半
            Gf2kVoleReceiverOutput receiverOutputHalf = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
            Gf2kVoleSenderOutput senderOutputHalf = Gf2kVoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            Gf2kVoleTestUtils.assertOutput(num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        Gf2kVoleSenderOutput senderOutput = Gf2kVoleSenderOutput.createEmpty();
        Gf2kVoleSenderOutput mergeSenderOutput = Gf2kVoleSenderOutput.createEmpty();
        Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createEmpty(delta);
        Gf2kVoleReceiverOutput mergeReceiverOutput = Gf2kVoleReceiverOutput.createEmpty(delta);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Gf2kVoleTestUtils.assertOutput(0, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        byte[] delta = new byte[GF2K_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createEmpty(delta);
        Gf2kVoleReceiverOutput mergeReceiverOutput = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Gf2kVoleSenderOutput senderOutput = Gf2kVoleSenderOutput.createEmpty();
        Gf2kVoleSenderOutput mergeSenderOutput = Gf2kVoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Gf2kVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        byte[] delta = new byte[GF2K_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Gf2kVoleReceiverOutput mergeReceiverOutput = Gf2kVoleReceiverOutput.createEmpty(delta);
        Gf2kVoleSenderOutput senderOutput = Gf2kVoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        Gf2kVoleSenderOutput mergeSenderOutput = Gf2kVoleSenderOutput.createEmpty();
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Gf2kVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        byte[] delta = new byte[GF2K_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleTestUtils.genReceiverOutput(num1, delta, SECURE_RANDOM);
        Gf2kVoleReceiverOutput mergeReceiverOutput = Gf2kVoleTestUtils.genReceiverOutput(num2, delta, SECURE_RANDOM);
        Gf2kVoleSenderOutput senderOutput = Gf2kVoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        Gf2kVoleSenderOutput mergeSenderOutput = Gf2kVoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Gf2kVoleTestUtils.assertOutput(num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        byte[] delta = new byte[GF2K_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        // 切分1比特
        Gf2kVoleReceiverOutput receiverOutput1 = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Gf2kVoleSenderOutput senderOutput1 = Gf2kVoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        Gf2kVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        Gf2kVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        Gf2kVoleTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        Gf2kVoleTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // 切分全部比特
        Gf2kVoleReceiverOutput receiverOutputAll = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
        Gf2kVoleSenderOutput senderOutputAll = Gf2kVoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        Gf2kVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        Gf2kVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        Gf2kVoleTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        Gf2kVoleTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // 切分n - 1比特
            Gf2kVoleReceiverOutput receiverOutputNum = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
            Gf2kVoleSenderOutput senderOutputNum = Gf2kVoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            Gf2kVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            Gf2kVoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            Gf2kVoleTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            Gf2kVoleTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // 切分一半比特
            Gf2kVoleReceiverOutput receiverOutputHalf = Gf2kVoleTestUtils.genReceiverOutput(num, delta, SECURE_RANDOM);
            Gf2kVoleSenderOutput senderOutputHalf = Gf2kVoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            Gf2kVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            Gf2kVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            Gf2kVoleTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            Gf2kVoleTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
