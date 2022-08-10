package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory.CoreCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13.Alsz13CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03.Iknp03CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15.Kos15CoreCotConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 核COT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
@RunWith(Parameterized.class)
public class CoreCotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreCotTest.class);
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
        Collection<Object[]> configurationParams = new ArrayList<>();
        // KOS15
        configurationParams.add(new Object[] {
            CoreCotType.KOS15.name(), new Kos15CoreCotConfig.Builder().build(),
        });
        // ALSZ13
        configurationParams.add(new Object[] {
            CoreCotType.ALSZ13.name(), new Alsz13CoreCotConfig.Builder().build(),
        });
        // IKNP03
        configurationParams.add(new Object[] {
            CoreCotType.IKNP03.name(), new Iknp03CoreCotConfig.Builder().build(),
        });

        return configurationParams;
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
    private final CoreCotConfig config;

    public CoreCotTest(String name, CoreCotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        CoreCotSender sender = CoreCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CoreCotReceiver receiver = CoreCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        CoreCotSender sender = CoreCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CoreCotReceiver receiver = CoreCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        CoreCotSender sender = CoreCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CoreCotReceiver receiver = CoreCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void testDefaultNum() {
        CoreCotSender sender = CoreCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CoreCotReceiver receiver = CoreCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        CoreCotSender sender = CoreCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CoreCotReceiver receiver = CoreCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        CoreCotSender sender = CoreCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CoreCotReceiver receiver = CoreCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        CoreCotSender sender = CoreCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CoreCotReceiver receiver = CoreCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM);
    }

    private void testPto(CoreCotSender sender, CoreCotReceiver receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            boolean[] choices = new boolean[num];
            IntStream.range(0, num).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            CoreCotSenderThread senderThread = new CoreCotSenderThread(sender, delta, num);
            CoreCotReceiverThread receiverThread = new CoreCotReceiverThread(receiver, choices);
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
        CoreCotSender sender = CoreCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        CoreCotReceiver receiver = CoreCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            CoreCotSenderThread senderThread = new CoreCotSenderThread(sender, delta, DEFAULT_NUM);
            CoreCotReceiverThread receiverThread = new CoreCotReceiverThread(receiver, choices);
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
            senderThread = new CoreCotSenderThread(sender, delta, DEFAULT_NUM);
            receiverThread = new CoreCotReceiverThread(receiver, choices);
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
