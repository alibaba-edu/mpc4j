package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zp64-VOLE输出测试。
 *
 * @author Hanwen Feng
 * @date 2022/6/15
 */
public class Zp64VoleOutputTest {
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
    private static final long PRIME = ZpManager.getPrime(62).longValue();
    /**
     * 另一个素数域
     */
    private static final long AN_PRIME = ZpManager.getPrime(32).longValue();
    /**
     * 关联值Δ = 0
     */
    private static final long ZERO_DELTA = 0L;
    /**
     * 关联值Δ = 2^k - 1
     */
    private static final long ONE_DELTA = 1L << (LongUtils.ceilLog2(PRIME) - 1);

    @Test
    public void testIllegalInputs() {
        try {
            // 创建长度为0的接收方输出
            Zp64VoleReceiverOutput.create(PRIME, ONE_DELTA, new long[0]);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ为负数的接收方输出
            long delta = -1L;
            long[] q = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            Zp64VoleReceiverOutput.create(PRIME, delta, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with negative Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ不合法的接收方输出
            long delta = PRIME - 1L;
            long[] q = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            Zp64VoleReceiverOutput.create(PRIME, delta, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with illegal Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建Δ过大的接收方输出
            long delta = PRIME + 1L;
            long[] q = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            Zp64VoleReceiverOutput.create(PRIME, delta, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with large Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建q为负数的接收方输出
            long[] q = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> -LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            Zp64VoleReceiverOutput.create(PRIME, ONE_DELTA, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with negative q");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建q过大的接收方输出
            long[] q = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> PRIME + 1L)
                    .toArray();
            Zp64VoleReceiverOutput.create(PRIME, ONE_DELTA, q);
            throw new IllegalStateException("ERROR: successfully create ReceiverOutput with large q");
        } catch (AssertionError ignored) {

        }
        try {
            // 合并两个Δ不相等的接收方输出
            long[] q = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            Zp64VoleReceiverOutput receiverOutput0 = Zp64VoleReceiverOutput.create(PRIME, ONE_DELTA, q);
            Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleReceiverOutput.create(PRIME, ZERO_DELTA, q);
            receiverOutput0.merge(receiverOutput1);
            throw new IllegalStateException("ERROR: successfully merge ReceiverOutput with different Δ");
        } catch (AssertionError ignored) {

        }
        try {
            // 合并两个素数域不相等的接收方输出
            long[] q = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            Zp64VoleReceiverOutput receiverOutput0 = Zp64VoleReceiverOutput.create(PRIME, ONE_DELTA, q);
            Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleReceiverOutput.create(AN_PRIME, ONE_DELTA, q);
            receiverOutput0.merge(receiverOutput1);
            throw new IllegalStateException("ERROR: successfully merge ReceiverOutput with different prime");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建长度为0的发送方输出
            Zp64VoleSenderOutput.create(PRIME, new long[0], new long[0]);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with n = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x和t大小不匹配的发送方输出
            long[] x = IntStream.range(0, MIN_NUM)
                    .mapToLong(index -> Math.floorMod(SECURE_RANDOM.nextLong(), PRIME))
                    .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> Math.floorMod(SECURE_RANDOM.nextLong(), PRIME))
                    .toArray();
            Zp64VoleSenderOutput.create(PRIME, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with different array length");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x为负数的发送方输出
            long[] x = IntStream.range(0, MIN_NUM)
                    .mapToLong(index -> -Math.floorMod(SECURE_RANDOM.nextLong(), PRIME))
                    .toArray();
            long[] t = IntStream.range(0, MIN_NUM)
                    .mapToLong(index -> Math.floorMod(SECURE_RANDOM.nextLong(), PRIME))
                    .toArray();
            Zp64VoleSenderOutput.create(PRIME, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with negative x");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x过大的发送方输出
            long[] x = IntStream.range(0, MIN_NUM)
                    .mapToLong(index -> PRIME + 1L)
                    .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            Zp64VoleSenderOutput.create(PRIME, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with large x");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建t为负数的发送方输出
            long[] x = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> -LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            Zp64VoleSenderOutput.create(PRIME, x, t);
            throw new IllegalStateException("ERROR: successfully create SenderOutput with negative t");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建x过大的发送方输出
            long[] x = IntStream.range(0, MIN_NUM)
                    .mapToLong(index -> LongUtils.randomNonNegative(PRIME, SECURE_RANDOM))
                    .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                    .mapToLong(index -> PRIME + 1L)
                    .toArray();
            Zp64VoleSenderOutput.create(PRIME, x, t);
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
        long delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(PRIME) - 1), SECURE_RANDOM);
        // 减小到1
        Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput1 = Zp64VoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        Zp64VoleTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // 减小到相同长度
        Zp64VoleReceiverOutput receiverOutputAll = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutputAll = Zp64VoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        Zp64VoleTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // 减小num - 1
            Zp64VoleReceiverOutput receiverOutputNum = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
            Zp64VoleSenderOutput senderOutputNum = Zp64VoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            Zp64VoleTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // 减小到一半
            Zp64VoleReceiverOutput receiverOutputHalf = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
            Zp64VoleSenderOutput senderOutputHalf = Zp64VoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            Zp64VoleTestUtils.assertOutput(num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        long delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(PRIME) - 1), SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createEmpty(PRIME);
        Zp64VoleSenderOutput mergeSenderOutput = Zp64VoleSenderOutput.createEmpty(PRIME);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createEmpty(PRIME, delta);
        Zp64VoleReceiverOutput mergeReceiverOutput = Zp64VoleReceiverOutput.createEmpty(PRIME, delta);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Zp64VoleTestUtils.assertOutput(0, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        long delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(PRIME) - 1), SECURE_RANDOM);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createEmpty(PRIME, delta);
        Zp64VoleReceiverOutput mergeReceiverOutput = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createEmpty(PRIME);
        Zp64VoleSenderOutput mergeSenderOutput = Zp64VoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Zp64VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        long delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(PRIME) - 1), SECURE_RANDOM);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        Zp64VoleReceiverOutput mergeReceiverOutput = Zp64VoleReceiverOutput.createEmpty(PRIME, delta);
        Zp64VoleSenderOutput senderOutput = Zp64VoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        Zp64VoleSenderOutput mergeSenderOutput = Zp64VoleSenderOutput.createEmpty(PRIME);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Zp64VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        long delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(PRIME) - 1), SECURE_RANDOM);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleTestUtils.genReceiverOutput(PRIME, num1, delta, SECURE_RANDOM);
        Zp64VoleReceiverOutput mergeReceiverOutput = Zp64VoleTestUtils.genReceiverOutput(PRIME, num2, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput = Zp64VoleTestUtils.genSenderOutput(receiverOutput, SECURE_RANDOM);
        Zp64VoleSenderOutput mergeSenderOutput = Zp64VoleTestUtils.genSenderOutput(mergeReceiverOutput, SECURE_RANDOM);
        // 合并
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // 验证结果
        Zp64VoleTestUtils.assertOutput(num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        long delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(PRIME) - 1), SECURE_RANDOM);
        // 切分1比特
        Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutput1 = Zp64VoleTestUtils.genSenderOutput(receiverOutput1, SECURE_RANDOM);
        Zp64VoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        Zp64VoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        Zp64VoleTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        Zp64VoleTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // 切分全部比特
        Zp64VoleReceiverOutput receiverOutputAll = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
        Zp64VoleSenderOutput senderOutputAll = Zp64VoleTestUtils.genSenderOutput(receiverOutputAll, SECURE_RANDOM);
        Zp64VoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        Zp64VoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        Zp64VoleTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        Zp64VoleTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // 切分n - 1比特
            Zp64VoleReceiverOutput receiverOutputNum = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
            Zp64VoleSenderOutput senderOutputNum = Zp64VoleTestUtils.genSenderOutput(receiverOutputNum, SECURE_RANDOM);
            Zp64VoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            Zp64VoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            Zp64VoleTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            Zp64VoleTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // 切分一半比特
            Zp64VoleReceiverOutput receiverOutputHalf = Zp64VoleTestUtils.genReceiverOutput(PRIME, num, delta, SECURE_RANDOM);
            Zp64VoleSenderOutput senderOutputHalf = Zp64VoleTestUtils.genSenderOutput(receiverOutputHalf, SECURE_RANDOM);
            Zp64VoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            Zp64VoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            Zp64VoleTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            Zp64VoleTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
