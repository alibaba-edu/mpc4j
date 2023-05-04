package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16.Kos16ZpCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.ZpCoreVoleFactory.ZpCoreVoleType;
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

import java.math.BigInteger;
import java.security.SecureRandom;
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
public class ZpCoreVoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZpCoreVoleTest.class);
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
    private final ZpCoreVoleConfig config;

    public ZpCoreVoleTest(String name, ZpCoreVoleConfig config) {
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
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            ZpVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            ZpVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // verify results
            ZpVoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
        Zp zp = DEFAULT_ZP;
        ZpCoreVoleSender sender = ZpCoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZpCoreVoleReceiver receiver = ZpCoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            ZpVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            ZpVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            ZpVoleTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            // second round, reset Δ
            delta = zp.createRangeRandom(SECURE_RANDOM);
            x = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> zp.createRandom(SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            senderThread = new ZpCoreVoleSenderThread(sender, zp, x);
            receiverThread = new ZpCoreVoleReceiverThread(receiver, zp, delta, DEFAULT_NUM);
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
            ZpVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            ZpVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            ZpVoleTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
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
