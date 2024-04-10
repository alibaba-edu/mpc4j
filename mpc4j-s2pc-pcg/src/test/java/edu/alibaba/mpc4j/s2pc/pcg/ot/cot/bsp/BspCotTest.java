package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory.BspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20MaBspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20ShBspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotSenderOutput;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batched single-point COT tests.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
@RunWith(Parameterized.class)
public class BspCotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(BspCotTest.class);
    /**
     * default each num, we select an odd number and not with the format 2^k
     */
    private static final int DEFAULT_EACH_NUM = 9;
    /**
     * large each num
     */
    private static final int LARGE_EACH_NUM = 1 << 16;
    /**
     * default batch num,  we select an odd number and not with the format 2^k
     */
    private static final int DEFAULT_BATCH_NUM = 9;
    /**
     * large batch num
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
     * config
     */
    private final BspCotConfig config;

    public BspCotTest(String name, BspCotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testFirstAlpha() {
        //noinspection UnnecessaryLocalVariable
        int eachNum = DEFAULT_BATCH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> 0)
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testLastAlpha() {
        int eachNum = DEFAULT_BATCH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> eachNum - 1)
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test1EachNum() {
        int eachNum = 1;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test2EachNum() {
        int eachNum = 2;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test1BatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = 1;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test2BatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = 2;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testDefault() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testParallelDefault() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, true);
    }

    @Test
    public void testLargeBatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = LARGE_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testParallelLargeBatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = LARGE_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, true);
    }

    @Test
    public void testLargeEachNum() {
        int eachNum = LARGE_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testParallelLargeEachNum() {
        int eachNum = LARGE_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, true);
    }

    private void testPto(int[] alphaArray, int eachNum, boolean parallel) {
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
            BspCotSenderThread senderThread = new BspCotSenderThread(sender, delta, batchNum, eachNum);
            BspCotReceiverThread receiverThread = new BspCotReceiverThread(receiver, alphaArray, eachNum);
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
            assertOutput(batchNum, eachNum, senderOutput, receiverOutput);
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
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = DEFAULT_BATCH_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            int[] alphaArray = IntStream.range(0, batchNum)
                .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
                .toArray();
            CotSenderOutput preSenderOutput = CotTestUtils.genSenderOutput(
                BspCotFactory.getPrecomputeNum(config, batchNum, eachNum), delta, SECURE_RANDOM
            );
            CotReceiverOutput preReceiverOutput = CotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
            BspCotSenderThread senderThread
                = new BspCotSenderThread(sender, delta, alphaArray.length, eachNum, preSenderOutput);
            BspCotReceiverThread receiverThread
                = new BspCotReceiverThread(receiver, alphaArray, eachNum, preReceiverOutput);
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
            assertOutput(batchNum, eachNum, senderOutput, receiverOutput);
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
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = DEFAULT_BATCH_NUM;
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            int[] alphaArray = IntStream.range(0, batchNum)
                .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum)).toArray();
            // first round
            BspCotSenderThread senderThread = new BspCotSenderThread(sender, delta, alphaArray.length, eachNum);
            BspCotReceiverThread receiverThread = new BspCotReceiverThread(receiver, alphaArray, eachNum);
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
            assertOutput(batchNum, eachNum, senderOutput, receiverOutput);
            printAndResetRpc(firstTime);
            // second round, reset Δ
            SECURE_RANDOM.nextBytes(delta);
            alphaArray = IntStream.range(0, batchNum)
                .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
                .toArray();
            senderThread = new BspCotSenderThread(sender, delta, alphaArray.length, eachNum);
            receiverThread = new BspCotReceiverThread(receiver, alphaArray, eachNum);
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
            assertOutput(batchNum, eachNum, secondSenderOutput, secondReceiverOutput);
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

    private void assertOutput(int batchNum, int eachNum,
                              BspCotSenderOutput senderOutput, BspCotReceiverOutput receiverOutput) {
        Assert.assertEquals(batchNum, senderOutput.getBatchNum());
        Assert.assertEquals(batchNum, receiverOutput.getBatchNum());
        Assert.assertEquals(eachNum, senderOutput.getEachNum());
        Assert.assertEquals(eachNum, receiverOutput.getEachNum());
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            SspCotSenderOutput eachSenderOutput = senderOutput.get(batchIndex);
            SspCotReceiverOutput eachReceiverOutput = receiverOutput.get(batchIndex);
            Assert.assertEquals(eachNum, eachSenderOutput.getNum());
            Assert.assertEquals(eachNum, eachReceiverOutput.getNum());
            IntStream.range(0, eachNum).forEach(index -> {
                if (index == eachReceiverOutput.getAlpha()) {
                    Assert.assertArrayEquals(eachSenderOutput.getR1(index), eachReceiverOutput.getRb(index));
                } else {
                    Assert.assertArrayEquals(eachSenderOutput.getR0(index), eachReceiverOutput.getRb(index));
                }
            });
        });
    }
}
