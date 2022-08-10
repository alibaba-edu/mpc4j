package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.wykw21.Wykw21ShZ2BspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.Z2SspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.Z2SspVoleSenderOutput;
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
import java.util.stream.IntStream;

/**
 * Z2-BSP-VOLE协议测试。
 *
 * @author Weiran Liu
 * @date 2022/6/23
 */
@RunWith(Parameterized.class)
public class Z2BspVoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2BspVoleTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量，设置为既不是偶数、也不是2^k格式的数量
     */
    private static final int DEFAULT_NUM = 15;
    /**
     * 默认批处理数量，设置为既不是偶数、也不是2^k格式的数量
     */
    private static final int DEFAULT_BATCH = 999;
    /**
     * 较大批处理数量
     */
    private static final int LARGE_BATCH = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // WYKW21_SEMI_HONEST
        configurationParams.add(new Object[] {
            Z2BspVoleFactory.Z2BspVoleType.WYKW21_SEMI_HONEST.name(), new Wykw21ShZ2BspVoleConfig.Builder().build(),
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
    private final Z2BspVoleConfig config;

    public Z2BspVoleTest(String name, Z2BspVoleConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        Z2BspVoleSender sender = Z2BspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2BspVoleReceiver receiver = Z2BspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }
//
//    @Test
//    public void testFirstAlpha() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH).map(mIndex -> 0).toArray();
//        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
//    }
//
//    @Test
//    public void testLastAlpha() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH).map(mIndex -> DEFAULT_NUM - 1).toArray();
//        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
//    }
//
//    @Test
//    public void testDefaultBatch1Num() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        int n = 1;
//        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH).map(mIndex -> SECURE_RANDOM.nextInt(n)).toArray();
//        testPto(sender, receiver, alphaArray, n);
//    }
//
//    @Test
//    public void testDefaultBatch2Num() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        int n = 2;
//        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH).map(mIndex -> SECURE_RANDOM.nextInt(n)).toArray();
//        testPto(sender, receiver, alphaArray, n);
//    }
//
//    @Test
//    public void test1BatchDefaultNum() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        int m = 1;
//        int[] alphaArray = IntStream.range(0, m).map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM)).toArray();
//        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
//    }
//
//    @Test
//    public void test2BatchDefaultNum() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        int m = 2;
//        int[] alphaArray = IntStream.range(0, m).map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM)).toArray();
//        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
//    }
//
//    @Test
//    public void testDefaultBatchDefaultNum() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH).map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM)).toArray();
//        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
//    }
//
//    @Test
//    public void testParallelDefaultBatchDefaultNum() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        sender.setParallel(true);
//        receiver.setParallel(true);
//        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH).map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM)).toArray();
//        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
//    }
//
//    @Test
//    public void testLargeBatchDefaultNum() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        int[] alphaArray = IntStream.range(0, LARGE_BATCH).map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM)).toArray();
//        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
//    }
//
//    @Test
//    public void testParallelLargeBatchDefaultNum() {
//        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
//        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
//        sender.setParallel(true);
//        receiver.setParallel(true);
//        int[] alphaArray = IntStream.range(0, LARGE_BATCH).map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM)).toArray();
//        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
//    }
//
//    private void testPto(BspCotSender sender, BspCotReceiver receiver, int[] alphaArray, int num) {
//        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
//        sender.setTaskId(randomTaskId);
//        receiver.setTaskId(randomTaskId);
//        try {
//            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
//            int batch = alphaArray.length;
//            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
//            SECURE_RANDOM.nextBytes(delta);
//            BspCotSenderThread senderThread = new BspCotSenderThread(sender, delta, batch, num);
//            BspCotReceiverThread receiverThread = new BspCotReceiverThread(receiver, alphaArray, num);
//            StopWatch stopWatch = new StopWatch();
//            // 开始执行协议
//            stopWatch.start();
//            senderThread.start();
//            receiverThread.start();
//            senderThread.join();
//            receiverThread.join();
//            stopWatch.stop();
//            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
//            stopWatch.reset();
//            long senderByteLength = senderRpc.getSendByteLength();
//            long receiverByteLength = receiverRpc.getSendByteLength();
//            senderRpc.reset();
//            receiverRpc.reset();
//            BspCotSenderOutput senderOutput = senderThread.getSenderOutput();
//            BspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
//            // 验证结果
//            assertOutput(num, batch, senderOutput, receiverOutput);
//            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
//                senderByteLength, receiverByteLength, time
//            );
//            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    @Test
    public void testPrecomputeLargeBatchDefaultNum() {
        Z2BspVoleSender sender = Z2BspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2BspVoleReceiver receiver = Z2BspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            int batch = LARGE_BATCH;
            int num = DEFAULT_NUM;
            boolean delta = SECURE_RANDOM.nextBoolean();
            int[] alphaArray = IntStream.range(0, batch).map(mIndex -> SECURE_RANDOM.nextInt(num)).toArray();
            Z2VoleReceiverOutput preReceiverOutput = Z2VoleTestUtils.genReceiverOutput(
                Z2BspVoleFactory.getPrecomputeNum(config, batch, num), delta, SECURE_RANDOM
            );
            Z2VoleSenderOutput preSenderOutput = Z2VoleTestUtils.genSenderOutput(preReceiverOutput, SECURE_RANDOM);
            Z2BspVoleSenderThread senderThread = new Z2BspVoleSenderThread(sender, alphaArray, num, preSenderOutput);
            Z2BspVoleReceiverThread receiverThread = new Z2BspVoleReceiverThread(
                receiver, delta, alphaArray.length, num, preReceiverOutput
            );
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
            Z2BspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Z2BspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(batch, num, senderOutput, receiverOutput);
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
        Z2BspVoleSender sender = Z2BspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2BspVoleReceiver receiver = Z2BspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            boolean delta = SECURE_RANDOM.nextBoolean();
            int[] alphaArray = IntStream.range(0, DEFAULT_BATCH).map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM)).toArray();
            // 第一次执行
            Z2BspVoleSenderThread senderThread = new Z2BspVoleSenderThread(sender, alphaArray, DEFAULT_NUM);
            Z2BspVoleReceiverThread receiverThread = new Z2BspVoleReceiverThread(receiver, delta, alphaArray.length, DEFAULT_NUM);
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
            Z2BspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Z2BspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(DEFAULT_BATCH, DEFAULT_NUM, senderOutput, receiverOutput);
            // 第二次执行，重置Δ
            delta = !delta;
            alphaArray = IntStream.range(0, DEFAULT_BATCH).map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM)).toArray();
            senderThread = new Z2BspVoleSenderThread(sender, alphaArray, DEFAULT_NUM);
            receiverThread = new Z2BspVoleReceiverThread(receiver,  delta, alphaArray.length, DEFAULT_NUM);
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
            Z2BspVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Z2BspVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(DEFAULT_BATCH, DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
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

    private void assertOutput(int batch, int num, Z2BspVoleSenderOutput senderOutput, Z2BspVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(batch, senderOutput.getBatch());
        Assert.assertEquals(batch, receiverOutput.getBatch());
        // 验证各个子结果
        IntStream.range(0, batch).forEach(batchIndex -> {
            Z2SspVoleSenderOutput z2SspVoleSenderOutput = senderOutput.get(batchIndex);
            Z2SspVoleReceiverOutput z2SspVoleReceiverOutput = receiverOutput.get(batchIndex);
            Assert.assertEquals(num, z2SspVoleSenderOutput.getNum());
            Assert.assertEquals(num, z2SspVoleReceiverOutput.getNum());
            byte[] x = new byte[z2SspVoleSenderOutput.getByteNum()];
            int offset = z2SspVoleSenderOutput.getByteNum() * Byte.SIZE - z2SspVoleSenderOutput.getNum();
            BinaryUtils.setBoolean(x, offset + z2SspVoleSenderOutput.getAlpha(), true);
            byte[] qt = BytesUtils.xor(z2SspVoleSenderOutput.getT(), z2SspVoleReceiverOutput.getQ());
            byte[] xDelta = BytesUtils.and(x, z2SspVoleReceiverOutput.getDeltaBytes());
            Assert.assertArrayEquals(qt, xDelta);
        });
    }
}
