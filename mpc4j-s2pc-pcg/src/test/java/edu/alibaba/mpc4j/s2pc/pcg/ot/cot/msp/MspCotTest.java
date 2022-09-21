package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory.MspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MSP-COT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
@RunWith(Parameterized.class)
public class MspCotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MspCotTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认多点数量
     */
    private static final int DEFAULT_T = 1 << 4;
    /**
     * 默认密钥数量
     */
    private static final int DEFAULT_NUM = 1 << 10;
    /**
     * 较大多点数量
     */
    private static final int LARGE_T = 1 << 10;
    /**
     * 较大密钥数量
     */
    private static final int LARGE_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // BCG19_REG (Malicious)
        configurationParams.add(new Object[] {
            MspCotType.BCG19_REG.name() + " (Malicious)",
            new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // YWL20_UNI (Malicious)
        configurationParams.add(new Object[] {
            MspCotType.YWL20_UNI.name() + "(Malicious)",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // BCG19_REG (Semi-Honest)
        configurationParams.add(new Object[] {
            MspCotType.BCG19_REG.name() + " (Semi-Honest)",
            new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20_UNI (Semi-honest)
        configurationParams.add(new Object[] {
            MspCotType.YWL20_UNI.name() + "(Semi-Honest)",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
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
    private final MspCotConfig config;

    public MspCotTest(String name, MspCotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void testDefaultNum1T() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1, DEFAULT_NUM);
    }

    @Test
    public void testDefaultNum2T() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2, DEFAULT_NUM);
    }

    @Test
    public void test1Num1T() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1, 1);
    }

    @Test
    public void test2Num2T() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2, 2);
    }

    @Test
    public void testDefaultNumDefaultT() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_T, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNumDefaultT() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_T, DEFAULT_NUM);
    }

    @Test
    public void testLargeNumLargeT() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_T, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNumLargeT() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_T, LARGE_NUM);
    }

    private void testPto(MspCotSender sender, MspCotReceiver receiver, int t, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            MspCotSenderThread senderThread = new MspCotSenderThread(sender, delta, t, num);
            MspCotReceiverThread receiverThread = new MspCotReceiverThread(receiver, t, num);
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
            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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
    public void testPrecomputeLargeNumLargeT() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            int num = LARGE_NUM;
            int t = LARGE_T;
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            CotSenderOutput preSenderOutput = CotTestUtils.genSenderOutput(
                MspCotFactory.getPrecomputeNum(config, t, num), delta, SECURE_RANDOM
            );
            CotReceiverOutput preReceiverOutput = CotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
            MspCotSenderThread senderThread = new MspCotSenderThread(sender, delta, t, num, preSenderOutput);
            MspCotReceiverThread receiverThread = new MspCotReceiverThread(receiver, t, num, preReceiverOutput);
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
            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResetDelta() {
        MspCotSender sender = MspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            // 第一次执行
            MspCotSenderThread senderThread = new MspCotSenderThread(sender, delta, DEFAULT_T, DEFAULT_NUM);
            MspCotReceiverThread receiverThread = new MspCotReceiverThread(receiver, DEFAULT_T, DEFAULT_NUM);
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
            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            // 第二次执行，重置Δ
            SECURE_RANDOM.nextBytes(delta);
            senderThread = new MspCotSenderThread(sender, delta, DEFAULT_T, DEFAULT_NUM);
            receiverThread = new MspCotReceiverThread(receiver, DEFAULT_T, DEFAULT_NUM);
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
            MspCotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            // Δ应该不等
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondSenderOutput.getDelta()), ByteBuffer.wrap(senderOutput.getDelta())
            );
            // 通信量应该相等
            Assert.assertEquals(secondSenderByteLength, firstSenderByteLength);
            Assert.assertEquals(secondReceiverByteLength, firstReceiverByteLength);
            assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
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

    private void assertOutput(int num, MspCotSenderOutput senderOutput, MspCotReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        Set<Integer> alphaSet = Arrays.stream(receiverOutput.getAlphaArray()).boxed().collect(Collectors.toSet());
        IntStream.range(0, num).forEach(index -> {
            if (alphaSet.contains(index)) {
                Assert.assertArrayEquals(senderOutput.getR1(index), receiverOutput.getRb(index));
            } else {
                Assert.assertArrayEquals(senderOutput.getR0(index), receiverOutput.getRb(index));
            }
        });
    }
}
