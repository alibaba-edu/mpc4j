package edu.alibaba.mpc4j.s2pc.pcg.vole.zp;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * ZP-VOLE输出测试。
 *
 * @author Weiran Liu
 * @date 2022/6/14
 */
public class ZpVoleOutputTest {
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
     * 默认素数域
     */
    private static final BigInteger PRIME = ZpManager.getPrime(64);
    /**
     * 另一个素数域
     */
    private static final BigInteger AN_PRIME = ZpManager.getPrime(65);
    /**
     * 关联值Δ = 0
     */
    private static final BigInteger ZERO_DELTA = BigInteger.ZERO;
    /**
     * 关联值Δ = 2^k - 1
     */
    private static final BigInteger ONE_DELTA = BigInteger.ONE.shiftLeft(PRIME.bitLength() - 1);

    @Test
    public void testIllegalInputs() {
        try {
            // 创建长度为0的接收方输出
            ZpVoleReceiverOutput.create(PRIME, ONE_DELTA, new BigInteger[0]);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ为负数的接收方输出
            BigInteger delta = BigInteger.ONE.negate();
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(PRIME, delta, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with negative Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ不合法的接收方输出
            BigInteger delta = PRIME.subtract(BigInteger.ONE);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(PRIME, delta, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with illegal Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ过大的接收方输出
            BigInteger delta = PRIME.add(BigInteger.ONE);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(PRIME, delta, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with large Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建q为负数的接收方输出
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM).negate())
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(PRIME, ONE_DELTA, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with negative q");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建q过大的接收方输出
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> PRIME.add(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(PRIME, ONE_DELTA, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with large q");
        } catch (AssertionError ignored) {

        }
        try {
            // 合并两个Δ不相等的接收方输出
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput receiverOutput0 = ZpVoleReceiverOutput.create(PRIME, ONE_DELTA, q);
            ZpVoleReceiverOutput receiverOutput1 = ZpVoleReceiverOutput.create(PRIME, ZERO_DELTA, q);
            receiverOutput0.merge(receiverOutput1);
            throw new IllegalStateException("ERROR: successfully merge ReceiverOutput with different Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 合并两个素数域不相等的接收方输出
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput receiverOutput0 = ZpVoleReceiverOutput.create(PRIME, ONE_DELTA, q);
            ZpVoleReceiverOutput receiverOutput1 = ZpVoleReceiverOutput.create(AN_PRIME, ONE_DELTA, q);
            receiverOutput0.merge(receiverOutput1);
            throw new IllegalStateException("ERROR: successfully merge ReceiverOutput with different prime");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建长度为0的发送方输出
            ZpVoleSenderOutput.create(PRIME, new BigInteger[0], new BigInteger[0]);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x和t大小不匹配的发送方输出
            BigInteger[] x = IntStream.range(0, MIN_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(PRIME, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with different array length");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x为负数的发送方输出
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM).negate())
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(PRIME, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with negative x");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x过大的发送方输出
            BigInteger[] x = IntStream.range(0, MIN_NUM)
                .mapToObj(index -> PRIME.add(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(PRIME, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with large x");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建t为负数的发送方输出
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM).negate())
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(PRIME, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with negative t");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x过大的发送方输出
            BigInteger[] x = IntStream.range(0, MIN_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> PRIME.add(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(PRIME, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with large t");
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
        BigInteger delta = new BigInteger(PRIME.bitLength() - 1, SECURE_RANDOM);
        // 减小到1
        ZpVoleReceiverOutput receiverOutput1 = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput1 = ZpVoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        ZpVoleTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // 减小到相同长度
        ZpVoleReceiverOutput receiverOutputAll = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutputAll = ZpVoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        ZpVoleTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // 减小num - 1
            ZpVoleReceiverOutput receiverOutputNum = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
            ZpVoleSenderOutput senderOutputNum = ZpVoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            ZpVoleTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // 减小到一半
            ZpVoleReceiverOutput receiverOutputHalf = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
            ZpVoleSenderOutput senderOutputHalf = ZpVoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            ZpVoleTestUtils.assertOutput(num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        BigInteger delta = new BigInteger(PRIME.bitLength() - 1, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createEmpty(PRIME);
        ZpVoleSenderOutput mergeSenderOutput = ZpVoleSenderOutput.createEmpty(PRIME);
        ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createEmpty(PRIME, delta);
        ZpVoleReceiverOutput mergeReceiverOutput = ZpVoleReceiverOutput.createEmpty(PRIME, delta);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        ZpVoleTestUtils.assertOutput(0, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        BigInteger delta = new BigInteger(PRIME.bitLength() - 1, SECURE_RANDOM);
        ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createEmpty(PRIME, delta);
        ZpVoleReceiverOutput mergeReceiverOutput = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createEmpty(PRIME);
        ZpVoleSenderOutput mergeSenderOutput = ZpVoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        ZpVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        BigInteger delta = new BigInteger(PRIME.bitLength() - 1, SECURE_RANDOM);
        ZpVoleReceiverOutput receiverOutput = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        ZpVoleReceiverOutput mergeReceiverOutput = ZpVoleReceiverOutput.createEmpty(PRIME, delta);
        ZpVoleSenderOutput senderOutput = ZpVoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        ZpVoleSenderOutput mergeSenderOutput = ZpVoleSenderOutput.createEmpty(PRIME);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        ZpVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        BigInteger delta = new BigInteger(PRIME.bitLength() - 1, SECURE_RANDOM);
        ZpVoleReceiverOutput receiverOutput = ZpVoleTestUtils.genReceiverOutput(PRIME, num1, delta, SECURE_RANDOM);
        ZpVoleReceiverOutput mergeReceiverOutput = ZpVoleTestUtils.genReceiverOutput(PRIME, num2, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput = ZpVoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        ZpVoleSenderOutput mergeSenderOutput = ZpVoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        ZpVoleTestUtils.assertOutput(num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        BigInteger delta = new BigInteger(PRIME.bitLength() - 1, SECURE_RANDOM);
        // 切分1比特
        ZpVoleReceiverOutput receiverOutput1 = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutput1 = ZpVoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        ZpVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        ZpVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        ZpVoleTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        ZpVoleTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // 切分全部比特
        ZpVoleReceiverOutput receiverOutputAll = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        ZpVoleSenderOutput senderOutputAll = ZpVoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        ZpVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        ZpVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        ZpVoleTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        ZpVoleTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // 切分n - 1比特
            ZpVoleReceiverOutput receiverOutputNum = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
            ZpVoleSenderOutput senderOutputNum = ZpVoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            ZpVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            ZpVoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            ZpVoleTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            ZpVoleTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // 切分一半比特
            ZpVoleReceiverOutput receiverOutputHalf = ZpVoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
            ZpVoleSenderOutput senderOutputHalf = ZpVoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            ZpVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            ZpVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            ZpVoleTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            ZpVoleTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
