package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory.BspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20MaBspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20ShBspCotConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BSP-COT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
@RunWith(Parameterized.class)
public class BspCotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BspCotTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量，设置为既不是偶数、也不是2^k格式的数量
     */
    private static final int DEFAULT_NUM = 9;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * 默认批处理数量，设置为既不是偶数、也不是2^k格式的数量
     */
    private static final int DEFAULT_BATCH_NUM = 9;
    /**
     * 较大批处理数量
     */
    private static final int LARGE_BATCH_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // YWL20_MALICIOUS
        configurationParams.add(new Object[] {
            BspCotType.YWL20_MALICIOUS.name(), new Ywl20MaBspCotConfig.Builder().build(),
        });
        // YWL20_SEMI_HONEST
        configurationParams.add(new Object[] {
            BspCotType.YWL20_SEMI_HONEST.name(), new Ywl20ShBspCotConfig.Builder().build(),
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
    private final BspCotConfig config;

    public BspCotTest(String name, BspCotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void testFirstAlpha() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> 0)
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
    }

    @Test
    public void testLastAlpha() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> DEFAULT_NUM - 1)
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
    }

    @Test
    public void test1Num() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int num = 1;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM).map(mIndex -> SECURE_RANDOM.nextInt(num)).toArray();
        testPto(sender, receiver, alphaArray, num);
    }

    @Test
    public void test2Num() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int num = 2;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM).map(mIndex -> SECURE_RANDOM.nextInt(num)).toArray();
        testPto(sender, receiver, alphaArray, num);
    }

    @Test
    public void test1BatchNum() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int batchNum = 1;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
    }

    @Test
    public void test2BatchNum() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int batchNum = 2;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
    }

    @Test
    public void testDefault() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefault() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
    }

    @Test
    public void testLargeBatch() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
    }

    @Test
    public void testParallelLargeBatch() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_NUM))
            .toArray();
        testPto(sender, receiver, alphaArray, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_NUM))
            .toArray();
        testPto(sender, receiver, alphaArray, LARGE_NUM);
    }

    private void testPto(BspCotSender sender, BspCotReceiver receiver, int[] alphaArray, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int batchNum = alphaArray.length;
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            BspCotSenderThread senderThread = new BspCotSenderThread(sender, delta, batchNum, num);
            BspCotReceiverThread receiverThread = new BspCotReceiverThread(receiver, alphaArray, num);
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
            BspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            BspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(batchNum, num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPrecompute() {
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int batchNum = DEFAULT_BATCH_NUM;
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            int[] alphaArray = IntStream.range(0, batchNum)
                .map(mIndex -> SECURE_RANDOM.nextInt(num))
                .toArray();
            CotSenderOutput preSenderOutput = CotTestUtils.genSenderOutput(
                BspCotFactory.getPrecomputeNum(config, batchNum, num), delta, SECURE_RANDOM
            );
            CotReceiverOutput preReceiverOutput = CotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
            BspCotSenderThread senderThread
                = new BspCotSenderThread(sender, delta, alphaArray.length, num, preSenderOutput);
            BspCotReceiverThread receiverThread
                = new BspCotReceiverThread(receiver, alphaArray, num, preReceiverOutput);
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
            BspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            BspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(batchNum, num, senderOutput, receiverOutput);
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
        BspCotSender sender = BspCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int batchNum = DEFAULT_BATCH_NUM;
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            int[] alphaArray = IntStream.range(0, batchNum)
                .map(mIndex -> SECURE_RANDOM.nextInt(num)).toArray();
            // 第一次执行
            BspCotSenderThread senderThread = new BspCotSenderThread(sender, delta, alphaArray.length, num);
            BspCotReceiverThread receiverThread = new BspCotReceiverThread(receiver, alphaArray, num);
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
            BspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            BspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, num, senderOutput, receiverOutput);
            // 第二次执行，重置Δ
            SECURE_RANDOM.nextBytes(delta);
            alphaArray = IntStream.range(0, batchNum)
                .map(mIndex -> SECURE_RANDOM.nextInt(num))
                .toArray();
            senderThread = new BspCotSenderThread(sender, delta, alphaArray.length, num);
            receiverThread = new BspCotReceiverThread(receiver, alphaArray, num);
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
            BspCotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            BspCotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            // Δ应该不等
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondSenderOutput.getDelta()), ByteBuffer.wrap(senderOutput.getDelta())
            );
            // 通信量应该相等
            Assert.assertEquals(secondSenderByteLength, firstSenderByteLength);
            Assert.assertEquals(secondReceiverByteLength, firstReceiverByteLength);
            assertOutput(batchNum, num, secondSenderOutput, secondReceiverOutput);
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

    private void assertOutput(int batchNum, int num,
                              BspCotSenderOutput senderOutput, BspCotReceiverOutput receiverOutput) {
        Assert.assertEquals(batchNum, senderOutput.getBatch());
        Assert.assertEquals(batchNum, receiverOutput.getBatch());
        // 验证各个子结果
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            SspCotSenderOutput sspcotSenderOutput = senderOutput.get(batchIndex);
            SspCotReceiverOutput sspcotReceiverOutput = receiverOutput.get(batchIndex);
            Assert.assertEquals(num, sspcotSenderOutput.getNum());
            Assert.assertEquals(num, sspcotReceiverOutput.getNum());
            IntStream.range(0, num).forEach(index -> {
                if (index == sspcotReceiverOutput.getAlpha()) {
                    Assert.assertArrayEquals(sspcotSenderOutput.getR1(index), sspcotReceiverOutput.getRb(index));
                } else {
                    Assert.assertArrayEquals(sspcotSenderOutput.getR0(index), sspcotReceiverOutput.getRb(index));
                }
            });
        });
    }
}
