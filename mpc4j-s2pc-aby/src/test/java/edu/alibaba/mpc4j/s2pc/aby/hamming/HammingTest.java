package edu.alibaba.mpc4j.s2pc.aby.hamming;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;
import edu.alibaba.mpc4j.s2pc.aby.hamming.bcp13.Bcp13ShHammingConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 汉明距离协议测试。
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
@RunWith(Parameterized.class)
public class HammingTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HammingTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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
            HammingFactory.HammingType.BCP13_SEMI_HONEST.name(), new Bcp13ShHammingConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * 发送方
     */
    private final Rpc senderRpc;
    /**
     * 接收方
     */
    private final Rpc receiverRpc;
    /**
     * 协议类型
     */
    private final HammingConfig config;

    public HammingTest(String name, HammingConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        HammingParty sender = HammingFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        HammingParty sender = HammingFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        HammingParty sender = HammingFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void test8Num() {
        HammingParty sender = HammingFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 8);
    }

    @Test
    public void testDefaultNum() {
        HammingParty sender = HammingFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        HammingParty sender = HammingFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        HammingParty sender = HammingFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        HammingParty sender = HammingFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM);
    }

    private void testPto(HammingParty sender, HammingParty receiver, int num) {
        testAllZeroInputPto(sender, receiver, num);
        testAllOneInputPto(sender, receiver, num);
        testRandomInputPto(sender, receiver, num);
    }

    private void testAllZeroInputPto(HammingParty sender, HammingParty receiver, int num) {
        int byteLength = CommonUtils.getByteLength(num);
        byte[] x0Bytes = new byte[byteLength];
        byte[] x1Bytes = new byte[byteLength];
        testInputPto(sender, receiver, x0Bytes, x1Bytes, num, 0);
    }

    private void testAllOneInputPto(HammingParty sender, HammingParty receiver, int num) {
        int byteLength = CommonUtils.getByteLength(num);
        byte[] x0Bytes = new byte[byteLength];
        Arrays.fill(x0Bytes, (byte)0xFF);
        BytesUtils.reduceByteArray(x0Bytes, num);
        byte[] x1Bytes = new byte[byteLength];
        Arrays.fill(x1Bytes, (byte)0xFF);
        BytesUtils.reduceByteArray(x1Bytes, num);
        testInputPto(sender, receiver, x0Bytes, x1Bytes, num, 0);
    }

    private void testRandomInputPto(HammingParty sender, HammingParty receiver, int num) {
        int byteLength = CommonUtils.getByteLength(num);
        byte[] x0Bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(x0Bytes);
        BytesUtils.reduceByteArray(x0Bytes, num);
        byte[] x1Bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(x1Bytes);
        BytesUtils.reduceByteArray(x1Bytes, num);
        int hammingDistance = BytesUtils.hammingDistance(x0Bytes, x1Bytes);
        testInputPto(sender, receiver, x0Bytes, x1Bytes, num, hammingDistance);
    }

    private void testInputPto(HammingParty sender, HammingParty receiver,
                              byte[] x0Bytes, byte[] x1Bytes, int num, int expectHammingDistance) {
        assert BytesUtils.isReduceByteArray(x0Bytes, num);
        assert BytesUtils.isReduceByteArray(x1Bytes, num);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);

        SquareSbitVector x0 = SquareSbitVector.create(num, x0Bytes, false);
        SquareSbitVector x1 = SquareSbitVector.create(num, x1Bytes, false);
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
            long senderByteLength = senderRpc.getSendByteLength();
            long receiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            int senderHammingDistance = senderThread.getHammingDistance();
            int receiverHammingDistance = receiverThread.getHammingDistance();
            // 验证结果
            assertOutput(expectHammingDistance, senderHammingDistance, receiverHammingDistance);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int expectHammingDistance, int senderHammingDistance, int receiverHammingDistance) {
//        Assert.assertEquals(expectHammingDistance, senderHammingDistance);
        Assert.assertEquals(expectHammingDistance, receiverHammingDistance);
    }
}
