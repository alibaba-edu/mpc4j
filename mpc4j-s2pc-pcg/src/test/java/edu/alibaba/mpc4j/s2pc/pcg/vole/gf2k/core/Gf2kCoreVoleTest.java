package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory.Gf2kCoreVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16.Kos16Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21.Wykw21Gf2kCoreVoleConfig;
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
 * GF2K-core VOLE tests.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
@RunWith(Parameterized.class)
public class Gf2kCoreVoleTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kCoreVoleTest.class);
    /**
     * GF2K
     */
    private static final Gf2k GF2K = Gf2kFactory.createInstance(EnvType.STANDARD);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1001;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // WYKW21
        configurations.add(
            new Object[]{Gf2kCoreVoleType.WYKW21.name(), new Wykw21Gf2kCoreVoleConfig.Builder().build(),}
        );
        // KOS16
        configurations.add(
            new Object[]{Gf2kCoreVoleType.KOS16.name(), new Kos16Gf2kCoreVoleConfig.Builder().build(),}
        );

        return configurations;
    }

    /**
     * the protocol config
     */
    private final Gf2kCoreVoleConfig config;

    public Gf2kCoreVoleTest(String name, Gf2kCoreVoleConfig config) {
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
    public void testDefault() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testLargePrime() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelLargePrime() {
        testPto(DEFAULT_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        Gf2kCoreVoleSender sender = Gf2kCoreVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kCoreVoleReceiver receiver = Gf2kCoreVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            // Δ is in [0, 2^k)
            byte[] delta = GF2K.createRangeRandom(SECURE_RANDOM);
            byte[][] x = IntStream.range(0, num)
                .mapToObj(index -> GF2K.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2kCoreVoleSenderThread senderThread = new Gf2kCoreVoleSenderThread(sender, x);
            Gf2kCoreVoleReceiverThread receiverThread = new Gf2kCoreVoleReceiverThread(receiver, delta, num);
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
            Gf2kVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            Gf2kVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        Gf2kCoreVoleSender sender = Gf2kCoreVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kCoreVoleReceiver receiver = Gf2kCoreVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            // Δ is in [0, 2^k)
            byte[] delta = GF2K.createRangeRandom(SECURE_RANDOM);
            byte[][] x = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> GF2K.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            // first round
            Gf2kCoreVoleSenderThread senderThread = new Gf2kCoreVoleSenderThread(sender, x);
            Gf2kCoreVoleReceiverThread receiverThread = new Gf2kCoreVoleReceiverThread(receiver, delta, DEFAULT_NUM);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            Gf2kVoleTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            printAndResetRpc(firstTime);
            // second round, reset Δ
            delta = GF2K.createRangeRandom(SECURE_RANDOM);
            x = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> GF2K.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            senderThread = new Gf2kCoreVoleSenderThread(sender, x);
            receiverThread = new Gf2kCoreVoleReceiverThread(receiver, delta, DEFAULT_NUM);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Gf2kVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            Gf2kVoleTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
            // Δ should be different
            Assert.assertNotEquals(secondReceiverOutput.getDelta(), receiverOutput.getDelta());
            printAndResetRpc(secondTime);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (reset Δ) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
