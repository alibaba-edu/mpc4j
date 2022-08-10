package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.wykw21.Wykw21ShZ2SspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;
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
 * Z2-SSP-VOLE测试。
 *
 * @author Weiran Liu
 * @date 2022/6/17
 */
@RunWith(Parameterized.class)
public class Z2SspVoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2SspVoleTest.class);
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
        // WYKW21_SEMI_HONEST
        configurationParams.add(new Object[] {
            Z2SspVoleFactory.Z2SspVoleType.WYKW21_SEMI_HONEST.name(), new Wykw21ShZ2SspVoleConfig.Builder().build(),
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
    private final Z2SspVoleConfig config;

    public Z2SspVoleTest(String name, Z2SspVoleConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void testFirstAlpha() {
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 0, DEFAULT_NUM);
    }

    @Test
    public void testLastAlpha() {
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver,DEFAULT_NUM - 1, DEFAULT_NUM);
    }

    @Test
    public void test1Num() {
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int num = 1;
        //noinspection ConstantConditions
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(sender, receiver, alpha, num);
    }

    @Test
    public void test2Num() {
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int num = 2;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(sender, receiver, alpha, num);
    }

    @Test
    public void testDefaultNum() {
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int alpha = SECURE_RANDOM.nextInt(DEFAULT_NUM);
        testPto(sender, receiver, alpha, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int alpha = SECURE_RANDOM.nextInt(DEFAULT_NUM);
        testPto(sender, receiver, alpha, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int alpha = SECURE_RANDOM.nextInt(LARGE_NUM);
        testPto(sender, receiver, alpha, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int alpha = SECURE_RANDOM.nextInt(LARGE_NUM);
        testPto(sender, receiver, alpha, LARGE_NUM);
    }

    private void testPto(Z2SspVoleSender sender, Z2SspVoleReceiver receiver, int alpha, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            boolean delta = SECURE_RANDOM.nextBoolean();
            Z2SspVoleSenderThread senderThread = new Z2SspVoleSenderThread(sender, alpha, num);
            Z2SspVoleReceiverThread receiverThread = new Z2SspVoleReceiverThread(receiver, delta, num);
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
            Z2SspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Z2SspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = LARGE_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            boolean delta = SECURE_RANDOM.nextBoolean();
            int alpha = SECURE_RANDOM.nextInt(num);
            Z2VoleReceiverOutput preReceiverOutput = Z2VoleTestUtils.genReceiverOutput(
                Z2SspVoleFactory.getPrecomputeNum(config, num), delta, SECURE_RANDOM
            );
            Z2VoleSenderOutput preSenderOutput = Z2VoleTestUtils.genSenderOutput(preReceiverOutput, SECURE_RANDOM);
            Z2SspVoleSenderThread senderThread = new Z2SspVoleSenderThread(sender, alpha, num, preSenderOutput);
            Z2SspVoleReceiverThread receiverThread = new Z2SspVoleReceiverThread(receiver, delta, num, preReceiverOutput);
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
            Z2SspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Z2SspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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
        Z2SspVoleSender sender = Z2SspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2SspVoleReceiver receiver = Z2SspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            boolean delta = SECURE_RANDOM.nextBoolean();
            int alpha = SECURE_RANDOM.nextInt(num);
            // 第一次执行
            Z2SspVoleSenderThread senderThread = new Z2SspVoleSenderThread(sender, alpha, num);
            Z2SspVoleReceiverThread receiverThread = new Z2SspVoleReceiverThread(receiver, delta, num);
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
            Z2SspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Z2SspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
            // 第二次执行，重置Δ
            delta = !delta;
            alpha = SECURE_RANDOM.nextInt(num);
            senderThread = new Z2SspVoleSenderThread(sender, alpha, num);
            receiverThread = new Z2SspVoleReceiverThread(receiver, delta, num);
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
            Z2SspVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Z2SspVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, secondSenderOutput, secondReceiverOutput);
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

    private void assertOutput(int num, Z2SspVoleSenderOutput senderOutput, Z2SspVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        byte[] x = new byte[senderOutput.getByteNum()];
        int offset = senderOutput.getByteNum() * Byte.SIZE - senderOutput.getNum();
        BinaryUtils.setBoolean(x, offset + senderOutput.getAlpha(), true);
        byte[] qt = BytesUtils.xor(senderOutput.getT(), receiverOutput.getQ());
        byte[] xDelta = BytesUtils.and(x, receiverOutput.getDeltaBytes());
        Assert.assertArrayEquals(qt, xDelta);
    }
}
