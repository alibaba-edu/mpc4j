package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16.Kos16ShZpCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.ZpCoreVoleFactory.ZpCoreVoleType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Zp-核VOLE协议测试。
 *
 * @author Hanwen Feng
 * @date 2022/06/10
 */
@RunWith(Parameterized.class)
public class ZpCoreVoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZpCoreVoleTest.class);
    /**
     * 随机状态\
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * 默认素数域
     */
    private static final BigInteger DEFAULT_PRIME = ZpManager.getPrime(32);
    /**
     * 较大素数域
     */
    private static final BigInteger LARGE_PRIME = ZpManager.getPrime(62);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // KOS16_SEMI_HONEST
        configurations.add(
            new Object[]{ZpCoreVoleType.KOS16_SEMI_HONEST.name(), new Kos16ShZpCoreVoleConfig.Builder().build(),}
        );

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
    private final ZpCoreVoleConfig config;

    public ZpCoreVoleTest(String name, ZpCoreVoleConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1, DEFAULT_PRIME);
    }

    @Test
    public void test2Num() {
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2, DEFAULT_PRIME);
    }

    @Test
    public void testDefault() {
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_PRIME);
    }

    @Test
    public void testParallelDefault() {
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_PRIME);
    }

    @Test
    public void testLargeNum() {
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM, DEFAULT_PRIME);
    }

    @Test
    public void testLargePrime() {
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, LARGE_PRIME);
    }

    @Test
    public void testParallelLargePrime() {
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM, LARGE_PRIME);
    }

    private void testPto(ZpCoreVoleSender sender, ZpCoreVoleReceiver receiver, int num, BigInteger prime) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            // Δ的取值范围是[0, 2^k)
            BigInteger delta = new BigInteger(prime.bitLength() - 1, SECURE_RANDOM);
            BigInteger[] x = IntStream.range(0, num)
                .mapToObj(index -> BigIntegerUtils.randomPositive(prime, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpCoreVoleSenderThread senderThread = new ZpCoreVoleSenderThread(sender, prime, x);
            ZpCoreVoleReceiverThread receiverThread = new ZpCoreVoleReceiverThread(receiver, prime, delta, num);
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
            ZpVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            ZpVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            ZpVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            // Δ的取值范围是[0, 2^k)
            BigInteger delta = new BigInteger(DEFAULT_PRIME.bitLength() - 1, SECURE_RANDOM);
            BigInteger[] x = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(DEFAULT_PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            // 第一次执行
            ZpCoreVoleSenderThread senderThread = new ZpCoreVoleSenderThread(sender, DEFAULT_PRIME, x);
            ZpCoreVoleReceiverThread receiverThread = new ZpCoreVoleReceiverThread(receiver, DEFAULT_PRIME, delta, DEFAULT_NUM);
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
            ZpVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            ZpVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            ZpVoleTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            // 第二次执行，重置Δ
            delta = new BigInteger(DEFAULT_PRIME.bitLength() - 1, SECURE_RANDOM);
            x = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> BigIntegerUtils.randomPositive(DEFAULT_PRIME, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            senderThread = new ZpCoreVoleSenderThread(sender, DEFAULT_PRIME, x);
            receiverThread = new ZpCoreVoleReceiverThread(receiver, DEFAULT_PRIME, delta, DEFAULT_NUM);
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
            ZpVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            ZpVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            ZpVoleTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
            // Δ应该不等
            Assert.assertNotEquals(secondReceiverOutput.getDelta(), receiverOutput.getDelta());
            // 通信量应该相等
            Assert.assertEquals(firstReceiverByteLength, secondReceiverByteLength);
            Assert.assertEquals(firstSenderByteLength, secondSenderByteLength);
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
