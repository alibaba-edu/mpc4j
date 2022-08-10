package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * COT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
@RunWith(Parameterized.class)
public class CotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CotTest.class);
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
        // DIRECT (Semi-honest)
        configurations.add(new Object[] {
            CotFactory.CotType.DIRECT.name() + " (Semi-honest)",
            new DirectCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // DIRECT (Malicious)
        configurations.add(new Object[] {
            CotFactory.CotType.DIRECT.name() + " (Malicious)",
            new DirectCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // CACHE (Semi-honest)
        configurations.add(new Object[] {
            CotFactory.CotType.CACHE.name() + " (Semi-honest)",
            new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // CACHE (Malicious)
        configurations.add(new Object[] {
            CotFactory.CotType.CACHE.name() + " (Malicious)",
            new CacheCotConfig.Builder(SecurityModel.MALICIOUS).build(),
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
    private final CotConfig config;

    public CotTest(String name, CotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        CotSender sender = CotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        CotSender sender = CotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        CotSender sender = CotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void testDefaultNum() {
        CotSender sender = CotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        CotSender sender = CotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        CotSender sender = CotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        CotSender sender = CotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM);
    }

    private void testPto(CotSender sender, CotReceiver receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            boolean[] choices = new boolean[num];
            IntStream.range(0, num).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            CotSenderThread senderThread = new CotSenderThread(sender, delta, num);
            CotReceiverThread receiverThread = new CotReceiverThread(receiver, choices);
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
            CotSenderOutput senderOutput = senderThread.getSenderOutput();
            CotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            CotTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        CotSender sender = CotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            boolean[] choices = new boolean[DEFAULT_NUM];
            IntStream.range(0, DEFAULT_NUM).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            // 第一次执行
            CotSenderThread senderThread = new CotSenderThread(sender, delta, DEFAULT_NUM);
            CotReceiverThread receiverThread = new CotReceiverThread(receiver, choices);
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
            CotSenderOutput senderOutput = senderThread.getSenderOutput();
            CotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            // 第二次执行，重置Δ
            SECURE_RANDOM.nextBytes(delta);
            IntStream.range(0, DEFAULT_NUM).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            senderThread = new CotSenderThread(sender, delta, DEFAULT_NUM);
            receiverThread = new CotReceiverThread(receiver, choices);
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
            CotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            CotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            // Δ应该不等
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondSenderOutput.getDelta()), ByteBuffer.wrap(senderOutput.getDelta())
            );
            // 通信量应该相等
            Assert.assertEquals(secondSenderByteLength, firstSenderByteLength);
            Assert.assertEquals(secondReceiverByteLength, firstReceiverByteLength);
            CotTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
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
