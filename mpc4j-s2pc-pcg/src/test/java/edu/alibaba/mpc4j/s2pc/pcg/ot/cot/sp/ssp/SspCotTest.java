package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20.Ywl20MaSspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20.Ywl20ShSspCotConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSP-COT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
@RunWith(Parameterized.class)
public class SspCotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SspCotTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量，设置为既不是偶数、也不是2^k格式的数量
     */
    private static final int DEFAULT_NUM = 999;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // YWL20_MALICIOUS
        configurationParams.add(new Object[] {
            SspCotType.YWL20_MALICIOUS.name(), new Ywl20MaSspCotConfig.Builder().build(),
        });
        // YWL20_SEMI_HONEST
        configurationParams.add(new Object[] {
            SspCotType.YWL20_SEMI_HONEST.name(), new Ywl20ShSspCotConfig.Builder().build(),
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
    private final SspCotConfig config;

    public SspCotTest(String name, SspCotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void testFirstAlpha() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 0, DEFAULT_NUM);
    }

    @Test
    public void testLastAlpha() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM - 1, DEFAULT_NUM);
    }

    @Test
    public void test1Num() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int num = 1;
        //noinspection ConstantConditions
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(sender, receiver, alpha, num);
    }

    @Test
    public void test2Num() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int num = 2;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(sender, receiver, alpha, num);
    }

    @Test
    public void testDefaultNum() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int alpha = SECURE_RANDOM.nextInt(DEFAULT_NUM);
        testPto(sender, receiver, alpha, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int alpha = SECURE_RANDOM.nextInt(DEFAULT_NUM);
        testPto(sender, receiver, alpha, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int alpha = SECURE_RANDOM.nextInt(LARGE_NUM);
        testPto(sender, receiver, alpha, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int alpha = SECURE_RANDOM.nextInt(LARGE_NUM);
        testPto(sender, receiver, alpha, LARGE_NUM);
    }

    private void testPto(SspCotSender sender, SspCotReceiver receiver, int alpha, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            SspCotSenderThread senderThread = new SspCotSenderThread(sender, delta, num);
            SspCotReceiverThread receiverThread = new SspCotReceiverThread(receiver, alpha, num);
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
            SspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            SspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPrecomputeLargeNum() {
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = LARGE_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            int alpha = SECURE_RANDOM.nextInt(num);
            CotSenderOutput preSenderOutput = CotTestUtils.genSenderOutput(
                SspCotFactory.getPrecomputeNum(config, num), delta, SECURE_RANDOM
            );
            CotReceiverOutput preReceiverOutput = CotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
            SspCotSenderThread senderThread = new SspCotSenderThread(sender, delta, num, preSenderOutput);
            SspCotReceiverThread receiverThread = new SspCotReceiverThread(receiver, alpha, num, preReceiverOutput);
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
            SspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            SspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(num, senderOutput, receiverOutput);
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
        SspCotSender sender = SspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            int alpha = SECURE_RANDOM.nextInt(num);
            // 第一次执行
            SspCotSenderThread senderThread = new SspCotSenderThread(sender, delta, num);
            SspCotReceiverThread receiverThread = new SspCotReceiverThread(receiver, alpha, num);
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
            SspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            SspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
            // 第二次执行，重置Δ
            SECURE_RANDOM.nextBytes(delta);
            alpha = SECURE_RANDOM.nextInt(num);
            senderThread = new SspCotSenderThread(sender, delta, num);
            receiverThread = new SspCotReceiverThread(receiver, alpha, num);
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
            SspCotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            SspCotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            // Δ应该不等
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondSenderOutput.getDelta()), ByteBuffer.wrap(senderOutput.getDelta())
            );
            // 通信量应该相等
            Assert.assertEquals(secondSenderByteLength, firstSenderByteLength);
            Assert.assertEquals(secondReceiverByteLength, firstReceiverByteLength);
            assertOutput(num, secondSenderOutput, secondReceiverOutput);
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

    private void assertOutput(int num, SspCotSenderOutput senderOutput, SspCotReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        IntStream.range(0, num).forEach(index -> {
            if (index == receiverOutput.getAlpha()) {
                Assert.assertArrayEquals(senderOutput.getR1(index), receiverOutput.getRb(index));
            } else {
                Assert.assertArrayEquals(senderOutput.getR0(index), receiverOutput.getRb(index));
            }
        });
    }
}
