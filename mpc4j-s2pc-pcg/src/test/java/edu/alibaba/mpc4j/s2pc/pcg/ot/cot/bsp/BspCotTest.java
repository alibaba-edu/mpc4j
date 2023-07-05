package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory.BspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20MaBspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20ShBspCotConfig;
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
public class BspCotTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BspCotTest.class);
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
        Collection<Object[]> configurations = new ArrayList<>();

        // YWL20_MALICIOUS
        configurations.add(new Object[]{
            BspCotType.YWL20_MALICIOUS.name(), new Ywl20MaBspCotConfig.Builder().build(),
        });
        // YWL20_SEMI_HONEST
        configurations.add(new Object[]{
            BspCotType.YWL20_SEMI_HONEST.name(), new Ywl20ShBspCotConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * 协议类型
     */
    private final BspCotConfig config;

    public BspCotTest(String name, BspCotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testFirstAlpha() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> 0)
            .toArray();
        testPto(alphaArray, DEFAULT_NUM, false);
    }

    @Test
    public void testLastAlpha() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> DEFAULT_NUM - 1)
            .toArray();
        testPto(alphaArray, DEFAULT_NUM, false);
    }

    @Test
    public void test1Num() {
        int num = 1;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(num))
            .toArray();
        testPto(alphaArray, num, false);
    }

    @Test
    public void test2Num() {
        int num = 2;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(num))
            .toArray();
        testPto(alphaArray, num, false);
    }

    @Test
    public void test1BatchNum() {
        int batchNum = 1;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_NUM, false);
    }

    @Test
    public void test2BatchNum() {
        int batchNum = 2;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_NUM, false);
    }

    @Test
    public void testDefault() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_NUM, true);
    }

    @Test
    public void testLargeBatch() {
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelLargeBatch() {
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_NUM))
            .toArray();
        testPto(alphaArray, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_NUM))
            .toArray();
        testPto(alphaArray, LARGE_NUM, true);
    }

    private void testPto(int[] alphaArray, int num, boolean parallel) {
        BspCotSender sender = BspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int batchNum = alphaArray.length;
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            BspCotSenderThread senderThread = new BspCotSenderThread(sender, delta, batchNum, num);
            BspCotReceiverThread receiverThread = new BspCotReceiverThread(receiver, alphaArray, num);
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            BspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            BspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPrecompute() {
        BspCotSender sender = BspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
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
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            BspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            BspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResetDelta() {
        BspCotSender sender = BspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BspCotReceiver receiver = BspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
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
            // first round
            BspCotSenderThread senderThread = new BspCotSenderThread(sender, delta, alphaArray.length, num);
            BspCotReceiverThread receiverThread = new BspCotReceiverThread(receiver, alphaArray, num);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            BspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            BspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, num, senderOutput, receiverOutput);
            printAndResetRpc(firstTime);
            // second round, reset Δ
            SECURE_RANDOM.nextBytes(delta);
            alphaArray = IntStream.range(0, batchNum)
                .map(mIndex -> SECURE_RANDOM.nextInt(num))
                .toArray();
            senderThread = new BspCotSenderThread(sender, delta, alphaArray.length, num);
            receiverThread = new BspCotReceiverThread(receiver, alphaArray, num);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            BspCotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            BspCotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, num, secondSenderOutput, secondReceiverOutput);
            // Δ should be different
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondSenderOutput.getDelta()), ByteBuffer.wrap(senderOutput.getDelta())
            );
            printAndResetRpc(secondTime);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (reset Δ) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int batchNum, int num,
                              BspCotSenderOutput senderOutput, BspCotReceiverOutput receiverOutput) {
        Assert.assertEquals(batchNum, senderOutput.getNum());
        Assert.assertEquals(batchNum, receiverOutput.getNum());
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
