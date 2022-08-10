package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core.kos16.Kos16ShZ2CoreVoleConfig;
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
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Z2-核VOLE协议测试。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
@RunWith(Parameterized.class)
public class Z2CoreVoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2CoreVoleTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // KOS16
        configurations.add(new Object[] {
            Z2CoreVoleFactory.Z2CoreVoleType.KOS16_SEMI_HONEST.name(), new Kos16ShZ2CoreVoleConfig.Builder().build(),
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
    private final Z2CoreVoleConfig config;

    public Z2CoreVoleTest(String name, Z2CoreVoleConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        Z2CoreVoleSender sender = Z2CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreVoleReceiver receiver = Z2CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        Z2CoreVoleSender sender = Z2CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreVoleReceiver receiver = Z2CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        Z2CoreVoleSender sender = Z2CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreVoleReceiver receiver = Z2CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void testDefaultNum() {
        Z2CoreVoleSender sender = Z2CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreVoleReceiver receiver = Z2CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        Z2CoreVoleSender sender = Z2CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreVoleReceiver receiver = Z2CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        Z2CoreVoleSender sender = Z2CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreVoleReceiver receiver = Z2CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        Z2CoreVoleSender sender = Z2CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreVoleReceiver receiver = Z2CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM);
    }

    private void testPto(Z2CoreVoleSender sender, Z2CoreVoleReceiver receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            boolean delta = SECURE_RANDOM.nextBoolean();
            int byteNum = CommonUtils.getByteLength(num);
            byte[] x = new byte[byteNum];
            SECURE_RANDOM.nextBytes(x);
            BytesUtils.reduceByteArray(x, num);
            Z2CoreVoleSenderThread senderThread = new Z2CoreVoleSenderThread(sender, x, num);
            Z2CoreVoleReceiverThread receiverThread = new Z2CoreVoleReceiverThread(receiver, delta, num);
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
            Z2VoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Z2VoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            Z2VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResetDelta() {
        Z2CoreVoleSender sender = Z2CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreVoleReceiver receiver = Z2CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            boolean delta = SECURE_RANDOM.nextBoolean();
            int byteNum = CommonUtils.getByteLength(DEFAULT_NUM);
            byte[] x = new byte[byteNum];
            SECURE_RANDOM.nextBytes(x);
            BytesUtils.reduceByteArray(x, DEFAULT_NUM);
            // 第一次执行
            Z2CoreVoleSenderThread senderThread = new Z2CoreVoleSenderThread(sender, x, DEFAULT_NUM);
            Z2CoreVoleReceiverThread receiverThread = new Z2CoreVoleReceiverThread(receiver, delta, DEFAULT_NUM);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long firstTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long firstSenderByteLength = senderRpc.getSendByteLength();
            long firstReceiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            Z2VoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Z2VoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            Z2VoleTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            // 第二次执行，重置Δ
            delta = !delta;
            SECURE_RANDOM.nextBytes(x);
            BytesUtils.reduceByteArray(x, DEFAULT_NUM);
            senderThread = new Z2CoreVoleSenderThread(sender, x, DEFAULT_NUM);
            receiverThread = new Z2CoreVoleReceiverThread(receiver, delta, DEFAULT_NUM);
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long secondTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long secondSenderByteLength = senderRpc.getSendByteLength();
            long secondReceiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            Z2VoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Z2VoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            Z2VoleTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
            // Δ应该不等
            Assert.assertNotEquals(secondReceiverOutput.getDelta(), receiverOutput.getDelta());
            // 通信量应该相等
            Assert.assertEquals(secondSenderByteLength, firstSenderByteLength);
            Assert.assertEquals(secondReceiverByteLength, firstReceiverByteLength);
            LOGGER.info("1st round, Send. {}B, Recv. {}B, {}ms",
                firstSenderByteLength, firstReceiverByteLength, firstTime
            );
            LOGGER.info("2nd round, Send. {}B, Recv. {}B, {}ms",
                secondSenderByteLength, secondReceiverByteLength, secondTime
            );
            LOGGER.info("-----test {} (reset Δ) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
