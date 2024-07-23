package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.gyw23.Gyw23SspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20.Ywl20ShSspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // GYW23_SEMI_HONEST
        configurations.add(new Object[]{
            SspCotType.GYW23_SEMI_HONEST.name(), new Gyw23SspCotConfig.Builder().build(),
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
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
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
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            int alpha = SECURE_RANDOM.nextInt(num);
            CotSenderOutput preSenderOutput = CotSenderOutput.createRandom(
                SspCotFactory.getPrecomputeNum(config, num), delta, SECURE_RANDOM
            );
            CotReceiverOutput preReceiverOutput = CotReceiverOutput.createRandom(preSenderOutput, SECURE_RANDOM);
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
