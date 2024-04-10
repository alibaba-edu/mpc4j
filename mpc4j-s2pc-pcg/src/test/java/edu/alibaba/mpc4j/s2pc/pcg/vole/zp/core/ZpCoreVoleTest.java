package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16.Kos16ZpCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.ZpCoreVoleFactory.ZpCoreVoleType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Zp-core VOLE tests.
 *
 * @author Hanwen Feng
 * @date 2022/06/10
 */
@RunWith(Parameterized.class)
public class ZpCoreVoleTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZpCoreVoleTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * the default Zp instance
     */
    private static final Zp DEFAULT_ZP = ZpFactory.createInstance(EnvType.STANDARD, 32);
    /**
     * the large Zp instance
     */
    private static final Zp LARGE_ZP = ZpFactory.createInstance(EnvType.STANDARD, 62);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // KOS16
        configurations.add(
            new Object[]{ZpCoreVoleType.KOS16.name(), new Kos16ZpCoreVoleConfig.Builder().build(),}
        );

        return configurations;
    }

    /**
     * the protocol config
     */
    private final ZpCoreVoleConfig config;

    public ZpCoreVoleTest(String name, ZpCoreVoleConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(1, DEFAULT_ZP, false);
    }

    @Test
    public void test2Num() {
        testPto(2, DEFAULT_ZP, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_NUM, DEFAULT_ZP, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, DEFAULT_ZP, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, DEFAULT_ZP, false);
    }

    @Test
    public void testLargePrime() {
        testPto(DEFAULT_NUM, LARGE_ZP, false);
    }

    @Test
    public void testParallelLargePrime() {
        testPto(DEFAULT_NUM, LARGE_ZP, true);
    }

    private void testPto(int num, Zp zp, boolean parallel) {
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            // Δ is in [0, 2^k)
            BigInteger delta = zp.createRangeRandom(SECURE_RANDOM);
            BigInteger[] x = IntStream.range(0, num)
                .mapToObj(index -> zp.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            ZpCoreVoleSenderThread senderThread = new ZpCoreVoleSenderThread(sender, zp, x);
            ZpCoreVoleReceiverThread receiverThread = new ZpCoreVoleReceiverThread(receiver, zp, delta, num);
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
            ZpVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            ZpVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            ZpVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        Zp zp = DEFAULT_ZP;
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            // Δ is in [0, 2^k)
            BigInteger delta = zp.createRangeRandom(SECURE_RANDOM);
            BigInteger[] x = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> zp.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            // first round
            ZpCoreVoleSenderThread senderThread = new ZpCoreVoleSenderThread(sender, zp, x);
            ZpCoreVoleReceiverThread receiverThread = new ZpCoreVoleReceiverThread(receiver, zp, delta, DEFAULT_NUM);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            ZpVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            ZpVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            ZpVoleTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            printAndResetRpc(firstTime);
            // second round, reset Δ
            delta = zp.createRangeRandom(SECURE_RANDOM);
            x = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> zp.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            senderThread = new ZpCoreVoleSenderThread(sender, zp, x);
            receiverThread = new ZpCoreVoleReceiverThread(receiver, zp, delta, DEFAULT_NUM);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            ZpVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            ZpVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            ZpVoleTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
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
