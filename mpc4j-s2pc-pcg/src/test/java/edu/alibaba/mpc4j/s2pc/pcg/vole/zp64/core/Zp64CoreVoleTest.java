package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.kos16.Kos16Zp64CoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.Zp64CoreVoleFactory.Zp64CoreVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleTestUtils;
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
 * Zp64-core VOLE tests.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
@RunWith(Parameterized.class)
public class Zp64CoreVoleTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zp64CoreVoleTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * the default Zp64 instance
     */
    private static final Zp64 DEFAULT_ZP64 = Zp64Factory.createInstance(EnvType.STANDARD, 32);
    /**
     * the large Zp64 instance
     */
    private static final Zp64 LARGE_ZP64 = Zp64Factory.createInstance(EnvType.STANDARD, 62);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // KOS16
        configurations.add(
            new Object[]{Zp64CoreVoleType.KOS16.name(), new Kos16Zp64CoreVoleConfig.Builder().build(),}
        );

        return configurations;
    }

    /**
     * the protocol config
     */
    private final Zp64CoreVoleConfig config;

    public Zp64CoreVoleTest(String name, Zp64CoreVoleConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(1, DEFAULT_ZP64, false);
    }

    @Test
    public void test2Num() {
        testPto(2, DEFAULT_ZP64, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_NUM, DEFAULT_ZP64, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, DEFAULT_ZP64, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, DEFAULT_ZP64, false);
    }

    @Test
    public void testLargePrime() {
        testPto(DEFAULT_NUM, LARGE_ZP64, false);
    }

    @Test
    public void testParallelLargePrime() {
        testPto(DEFAULT_NUM, LARGE_ZP64, true);
    }

    private void testPto(int num, Zp64 zp64, boolean parallel) {
        Zp64CoreVoleSender sender = Zp64CoreVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Zp64CoreVoleReceiver receiver = Zp64CoreVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            // Δ is in [0, 2^l)
            long delta = zp64.createRangeRandom(SECURE_RANDOM);
            long[] x = IntStream.range(0, num)
                .mapToLong(index -> zp64.createRandom(SECURE_RANDOM))
                .toArray();
            Zp64CoreVoleSenderThread senderThread = new Zp64CoreVoleSenderThread(sender, zp64, x);
            Zp64CoreVoleReceiverThread receiverThread = new Zp64CoreVoleReceiverThread(receiver, zp64, delta, num);
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
            Zp64VoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Zp64VoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            Zp64VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        Zp64 zp64 = DEFAULT_ZP64;
        Zp64CoreVoleSender sender = Zp64CoreVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Zp64CoreVoleReceiver receiver = Zp64CoreVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            // Δ is in [0, 2^l)
            long delta = zp64.createRangeRandom(SECURE_RANDOM);
            long[] x = IntStream.range(0, DEFAULT_NUM)
                .mapToLong(index -> zp64.createRandom(SECURE_RANDOM))
                .toArray();
            // first round
            Zp64CoreVoleSenderThread senderThread = new Zp64CoreVoleSenderThread(sender, zp64, x);
            Zp64CoreVoleReceiverThread receiverThread = new Zp64CoreVoleReceiverThread(receiver, zp64, delta, DEFAULT_NUM);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Zp64VoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Zp64VoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            Zp64VoleTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            printAndResetRpc(firstTime);
            // second round, reset Δ
            delta = zp64.createRangeRandom(SECURE_RANDOM);
            x = IntStream.range(0, DEFAULT_NUM)
                .mapToLong(index -> zp64.createRandom(SECURE_RANDOM))
                .toArray();
            senderThread = new Zp64CoreVoleSenderThread(sender, zp64, x);
            receiverThread = new Zp64CoreVoleReceiverThread(receiver, zp64, delta, DEFAULT_NUM);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Zp64VoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Zp64VoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            Zp64VoleTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
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
