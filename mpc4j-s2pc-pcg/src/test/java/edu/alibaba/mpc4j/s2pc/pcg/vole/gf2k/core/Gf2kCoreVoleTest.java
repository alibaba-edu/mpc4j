package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory.Gf2kCoreVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16.Kos16Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21.Wykw21Gf2kCoreVoleConfig;
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
 * GF2K-core VOLE tests.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
@RunWith(Parameterized.class)
public class Gf2kCoreVoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kCoreVoleTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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
    private final Gf2kCoreVoleConfig config;

    public Gf2kCoreVoleTest(String name, Gf2kCoreVoleConfig config) {
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
        Gf2kCoreVoleSender sender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Gf2kCoreVoleReceiver receiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            StopWatch stopWatch = new StopWatch();
            // start executing
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
            Gf2kVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // verify results
            Gf2kVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    @Test
    public void testResetDelta() {
        Gf2kCoreVoleSender sender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Gf2kCoreVoleReceiver receiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            Gf2kVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            Gf2kVoleTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            // second round, reset Δ
            delta = GF2K.createRangeRandom(SECURE_RANDOM);
            x = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> GF2K.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            senderThread = new Gf2kCoreVoleSenderThread(sender, x);
            receiverThread = new Gf2kCoreVoleReceiverThread(receiver, delta, DEFAULT_NUM);
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
            Gf2kVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Gf2kVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            Gf2kVoleTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }
}
