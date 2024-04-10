package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20.Ywl20ShSspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotFactory.SspCotType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Single single-point COT tests.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
@RunWith(Parameterized.class)
public class SspCotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SspCotTest.class);
    /**
     * default num, the num is not even, and not in format 2^k
     */
    private static final int DEFAULT_NUM = 9;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // YWL20_MALICIOUS
        configurations.add(new Object[]{
            SspCotType.YWL20_MALICIOUS.name(), new Ywl20ShSspCotConfig.Builder().build(),
        });
        // YWL20_SEMI_HONEST
        configurations.add(new Object[]{
            SspCotType.YWL20_SEMI_HONEST.name(), new Ywl20ShSspCotConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final SspCotConfig config;

    public SspCotTest(String name, SspCotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testFirstAlpha() {
        //noinspection UnnecessaryLocalVariable
        int num = DEFAULT_NUM;
        int alpha = 0;
        testPto(alpha, num, false);
    }

    @Test
    public void testLastAlpha() {
        int num = DEFAULT_NUM;
        int alpha = num - 1;
        testPto(alpha, num, false);
    }

    @Test
    public void test1Num() {
        int num = 1;
        int alpha = 0;
        testPto(alpha, num, false);
    }

    @Test
    public void test2Num() {
        int num = 2;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testDefault() {
        int num = DEFAULT_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testParallelDefault() {
        int num = DEFAULT_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, true);
    }

    @Test
    public void testLargeNum() {
        int num = LARGE_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testParallelLargeNum() {
        int num = LARGE_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, true);
    }

    private void testPto(int alpha, int num, boolean parallel) {
        SspCotSender sender = SspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            SspCotSenderThread senderThread = new SspCotSenderThread(sender, delta, num);
            SspCotReceiverThread receiverThread = new SspCotReceiverThread(receiver, alpha, num);
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
            SspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            SspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
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
        SspCotSender sender = SspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = DEFAULT_NUM;
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
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            SspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            SspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
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
        SspCotSender sender = SspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SspCotReceiver receiver = SspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            int alpha = SECURE_RANDOM.nextInt(num);
            // first round
            SspCotSenderThread senderThread = new SspCotSenderThread(sender, delta, num);
            SspCotReceiverThread receiverThread = new SspCotReceiverThread(receiver, alpha, num);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            SspCotSenderOutput firstSenderOutput = senderThread.getSenderOutput();
            SspCotReceiverOutput firstReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, firstSenderOutput, firstReceiverOutput);
            printAndResetRpc(firstTime);
            // second round, reset Δ
            SECURE_RANDOM.nextBytes(delta);
            alpha = SECURE_RANDOM.nextInt(num);
            senderThread = new SspCotSenderThread(sender, delta, num);
            receiverThread = new SspCotReceiverThread(receiver, alpha, num);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            SspCotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            SspCotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, secondSenderOutput, secondReceiverOutput);
            // Δ should be different
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondSenderOutput.getDelta()), ByteBuffer.wrap(firstSenderOutput.getDelta())
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
