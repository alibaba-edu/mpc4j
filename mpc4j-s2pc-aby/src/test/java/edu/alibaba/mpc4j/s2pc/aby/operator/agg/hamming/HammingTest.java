package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13.Bcp13ShHammingConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * hamming distance protocol test.
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
@RunWith(Parameterized.class)
public class HammingTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(HammingTest.class);
    /**
     * 默认运算数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 较大运算数量
     */
    private static final int LARGE_NUM = 1 << 18;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BCP13 (semi-honest)
        configurations.add(new Object[]{
            HammingFactory.HammingType.BCP13_SEMI_HONEST.name(),
            new Bcp13ShHammingConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final HammingConfig config;

    public HammingTest(String name, HammingConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(1, false);
    }

    @Test
    public void test2Num() {
        testPto(2, false);
    }

    @Test
    public void test8Num() {
        testPto(8, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        HammingParty sender = HammingFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        testAllZeroInputPto(sender, receiver, num);
        testAllOneInputPto(sender, receiver, num);
        testRandomInputPto(sender, receiver, num);
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void testAllZeroInputPto(HammingParty sender, HammingParty receiver, int num) {
        int byteLength = CommonUtils.getByteLength(num);
        byte[] x0Bytes = new byte[byteLength];
        byte[] x1Bytes = new byte[byteLength];
        testInputPto(sender, receiver, x0Bytes, x1Bytes, num);
    }

    private void testAllOneInputPto(HammingParty sender, HammingParty receiver, int num) {
        int byteLength = CommonUtils.getByteLength(num);
        byte[] x0Bytes = new byte[byteLength];
        Arrays.fill(x0Bytes, (byte) 0xFF);
        BytesUtils.reduceByteArray(x0Bytes, num);
        byte[] x1Bytes = new byte[byteLength];
        Arrays.fill(x1Bytes, (byte) 0xFF);
        BytesUtils.reduceByteArray(x1Bytes, num);
        testInputPto(sender, receiver, x0Bytes, x1Bytes, num);
    }

    private void testRandomInputPto(HammingParty sender, HammingParty receiver, int num) {
        int byteLength = CommonUtils.getByteLength(num);
        byte[] x0Bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(x0Bytes);
        BytesUtils.reduceByteArray(x0Bytes, num);
        byte[] x1Bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(x1Bytes);
        BytesUtils.reduceByteArray(x1Bytes, num);
        testInputPto(sender, receiver, x0Bytes, x1Bytes, num);
    }

    private void testInputPto(HammingParty sender, HammingParty receiver, byte[] x0Bytes, byte[] x1Bytes, int num) {
        int expectHammingDistance = BytesUtils.hammingDistance(x0Bytes, x1Bytes);
        assert BytesUtils.isReduceByteArray(x0Bytes, num);
        assert BytesUtils.isReduceByteArray(x1Bytes, num);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        SquareZ2Vector x0 = SquareZ2Vector.create(num, x0Bytes, false);
        SquareZ2Vector x1 = SquareZ2Vector.create(num, x1Bytes, false);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            HammingSenderThread senderThread = new HammingSenderThread(sender, x0);
            HammingReceiverThread receiverThread = new HammingReceiverThread(receiver, x1);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            int senderHammingDistance = senderThread.getHammingDistance();
            int receiverHammingDistance = receiverThread.getHammingDistance();
            assertOutput(expectHammingDistance, senderHammingDistance, receiverHammingDistance);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int expectHammingDistance, int senderHammingDistance, int receiverHammingDistance) {
        Assert.assertEquals(expectHammingDistance, senderHammingDistance);
        Assert.assertEquals(expectHammingDistance, receiverHammingDistance);
    }
}
