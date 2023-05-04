package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.kos16.Kos16Zp64CoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.Zp64CoreVoleFactory.Zp64CoreVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleTestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
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
public class Zp64CoreVoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zp64CoreVoleTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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
     * the sender rpc
     */
    private final Rpc senderRpc;
    /**
     * the receiver rpc
     */
    private final Rpc receiverRpc;
    /**
     * the protocol config
     */
    private final Zp64CoreVoleConfig config;

    public Zp64CoreVoleTest(String name, Zp64CoreVoleConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Before
    public void connect() {
        senderRpc.connect();
        receiverRpc.connect();
    }

    @After
    public void disconnect() {
        senderRpc.disconnect();
        receiverRpc.disconnect();
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
        Zp64CoreVoleSender sender = Zp64CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64CoreVoleReceiver receiver = Zp64CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            StopWatch stopWatch = new StopWatch();
            // start
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
            Zp64VoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Zp64VoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // verify
            Zp64VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
            sender.destroy();
            receiver.destroy();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResetDelta() {
        Zp64 zp64 = DEFAULT_ZP64;
        Zp64CoreVoleSender sender = Zp64CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64CoreVoleReceiver receiver = Zp64CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            Zp64VoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Zp64VoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            Zp64VoleTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            // second round, reset Δ
            delta = zp64.createRangeRandom(SECURE_RANDOM);
            x = IntStream.range(0, DEFAULT_NUM)
                .mapToLong(index -> zp64.createRandom(SECURE_RANDOM))
                .toArray();
            senderThread = new Zp64CoreVoleSenderThread(sender, zp64, x);
            receiverThread = new Zp64CoreVoleReceiverThread(receiver, zp64, delta, DEFAULT_NUM);
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
            Zp64VoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Zp64VoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            Zp64VoleTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
            // Δ should be unequal
            Assert.assertNotEquals(secondReceiverOutput.getDelta(), receiverOutput.getDelta());
            // communication should be equal
            Assert.assertEquals(firstReceiverByteLength, secondReceiverByteLength);
            Assert.assertEquals(firstSenderByteLength, secondSenderByteLength);
            LOGGER.info("1st round, Send. {}B, Recv. {}B, {}ms",
                firstSenderByteLength, firstReceiverByteLength, firstTime
            );
            LOGGER.info("2nd round, Send. {}B, Recv. {}B, {}ms",
                secondSenderByteLength, secondReceiverByteLength, secondTime
            );
            LOGGER.info("-----test {} (reset Δ) end-----", sender.getPtoDesc().getPtoName());
            sender.destroy();
            receiver.destroy();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
