package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
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
 * COT test.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
@RunWith(Parameterized.class)
public class CotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(CotTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // DIRECT
        configurations.add(new Object[] {
            CotFactory.CotType.DIRECT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        configurations.add(new Object[] {
            CotFactory.CotType.DIRECT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // CACHE
        configurations.add(new Object[] {
            CotFactory.CotType.CACHE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        configurations.add(new Object[] {
            CotFactory.CotType.CACHE.name() + " (" + SecurityModel.MALICIOUS + ")",
            new CacheCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });

        return configurations;
    }
    /**
     * 协议类型
     */
    private final CotConfig config;

    public CotTest(String name, CotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(1, false);
    }

    @Test
    public void test2Num() {
        testPto(2, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(1 << 20, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(1 << 20, true);
    }

    private void testPto(int num, boolean parallel) {
        CotSender sender = CotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            boolean[] choices = new boolean[num];
            IntStream.range(0, num).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            CotSenderThread senderThread = new CotSenderThread(sender, delta, num);
            CotReceiverThread receiverThread = new CotReceiverThread(receiver, choices);
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
            CotSenderOutput senderOutput = senderThread.getSenderOutput();
            CotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(num, senderOutput, receiverOutput);
            Assert.assertArrayEquals(delta, senderOutput.getDelta());
            Assert.assertArrayEquals(choices, receiverOutput.getChoices());
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
    public void testResetDelta() {
        CotSender sender = CotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            boolean[] choices = new boolean[DEFAULT_NUM];
            IntStream.range(0, DEFAULT_NUM).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            // first round
            CotSenderThread senderThread = new CotSenderThread(sender, delta, DEFAULT_NUM);
            CotReceiverThread receiverThread = new CotReceiverThread(receiver, choices);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            CotSenderOutput senderOutput = senderThread.getSenderOutput();
            CotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            printAndResetRpc(firstTime);
            // second round, reset Δ
            SECURE_RANDOM.nextBytes(delta);
            IntStream.range(0, DEFAULT_NUM).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            senderThread = new CotSenderThread(sender, delta, DEFAULT_NUM);
            receiverThread = new CotReceiverThread(receiver, choices);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            CotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            CotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
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

    @Test
    public void testLessUpdate() {
        int num = DEFAULT_NUM;
        CotSender sender = CotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (less update) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            boolean[] choices = new boolean[num];
            IntStream.range(0, DEFAULT_NUM).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            CotSenderThread senderThread = new CotSenderThread(sender, delta, num, num / 2 - 1);
            CotReceiverThread receiverThread = new CotReceiverThread(receiver, choices, num / 2 - 1);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            CotSenderOutput senderOutput = senderThread.getSenderOutput();
            CotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (less update) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
