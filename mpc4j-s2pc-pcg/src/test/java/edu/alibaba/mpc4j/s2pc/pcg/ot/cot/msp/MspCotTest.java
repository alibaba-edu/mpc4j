package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory.MspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MSP-COT tests.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
@RunWith(Parameterized.class)
public class MspCotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(MspCotTest.class);
    /**
     * default sparse num
     */
    private static final int DEFAULT_T = 1 << 4;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1 << 10;
    /**
     * large spare num
     */
    private static final int LARGE_T = 1 << 10;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BCG19_REG
        configurations.add(new Object[] {
            MspCotType.BCG19_REG.name() + " (" + SecurityModel.MALICIOUS.name() + ")",
            new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[] {
            MspCotType.BCG19_REG.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20_UNI
        configurations.add(new Object[] {
            MspCotType.YWL20_UNI.name() + " (" + SecurityModel.MALICIOUS.name() + ")",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[] {
            MspCotType.YWL20_UNI.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final MspCotConfig config;

    public MspCotTest(String name, MspCotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testDefaultNum1T() {
        testPto(1, DEFAULT_NUM, false);
    }

    @Test
    public void testDefaultNum2T() {
        testPto(2, DEFAULT_NUM, false);
    }

    @Test
    public void test1Num1T() {
        testPto(1, 1, false);
    }

    @Test
    public void test2Num2T() {
        testPto(2, 2, false);
    }

    @Test
    public void testDefaultNumDefaultT() {
        testPto(DEFAULT_T, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNumDefaultT() {
        testPto(DEFAULT_T, DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNumLargeT() {
        testPto(LARGE_T, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNumLargeT() {
        testPto(LARGE_T, LARGE_NUM, true);
    }

    private void testPto(int t, int num, boolean parallel) {
        MspCotSender sender = MspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            MspCotSenderThread senderThread = new MspCotSenderThread(sender, delta, t, num);
            MspCotReceiverThread receiverThread = new MspCotReceiverThread(receiver, t, num);
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
            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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
    public void testPrecomputeLargeNumLargeT() {
        MspCotSender sender = MspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
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
            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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
        MspCotSender sender = MspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            // first round
            MspCotSenderThread senderThread = new MspCotSenderThread(sender, delta, DEFAULT_T, DEFAULT_NUM);
            MspCotReceiverThread receiverThread = new MspCotReceiverThread(receiver, DEFAULT_T, DEFAULT_NUM);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            printAndResetRpc(firstTime);
            // second round, reset Δ
            SECURE_RANDOM.nextBytes(delta);
            senderThread = new MspCotSenderThread(sender, delta, DEFAULT_T, DEFAULT_NUM);
            receiverThread = new MspCotReceiverThread(receiver, DEFAULT_T, DEFAULT_NUM);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            MspCotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
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
