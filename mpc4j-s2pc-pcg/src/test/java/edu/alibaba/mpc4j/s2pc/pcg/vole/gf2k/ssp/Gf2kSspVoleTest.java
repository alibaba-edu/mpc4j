package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleFactory.Gf2kSspVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21.Wykw21Gf2kShSspVoleConfig;
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

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Single single-point GF2K VOLE tests.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
@RunWith(Parameterized.class)
public class Gf2kSspVoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kSspVoleTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * GF2K
     */
    private static final Gf2k GF2K = Gf2kFactory.createInstance(EnvType.STANDARD);
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

        // WYKW21_SEMI_HONEST
        configurations.add(new Object[]{
            Gf2kSspVoleType.WYKW21_SEMI_HONEST.name(), new Wykw21Gf2kShSspVoleConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * the sender RPC
     */
    private final Rpc senderRpc;
    /**
     * the receiver RPC
     */
    private final Rpc receiverRpc;
    /**
     * config
     */
    private final Gf2kSspVoleConfig config;

    public Gf2kSspVoleTest(String name, Gf2kSspVoleConfig config) {
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
        Gf2kSspVoleSender sender = Gf2kSspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Gf2kSspVoleReceiver receiver = Gf2kSspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = GF2K.createRangeRandom(SECURE_RANDOM);
            Gf2kSspVoleSenderThread senderThread = new Gf2kSspVoleSenderThread(sender, alpha, num);
            Gf2kSspVoleReceiverThread receiverThread = new Gf2kSspVoleReceiverThread(receiver, delta, num);
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
            Gf2kSspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kSspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(num, senderOutput, receiverOutput);
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
    public void testPrecompute() {
        Gf2kSspVoleSender sender = Gf2kSspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Gf2kSspVoleReceiver receiver = Gf2kSspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = GF2K.createRangeRandom(SECURE_RANDOM);
            int alpha = SECURE_RANDOM.nextInt(num);
            Gf2kVoleReceiverOutput preReceiverOutput = Gf2kVoleTestUtils.genReceiverOutput(
                Gf2kSspVoleFactory.getPrecomputeNum(config, num), delta, SECURE_RANDOM
            );
            Gf2kVoleSenderOutput preSenderOutput = Gf2kVoleTestUtils.genSenderOutput(preReceiverOutput, SECURE_RANDOM);
            Gf2kSspVoleSenderThread senderThread
                = new Gf2kSspVoleSenderThread(sender, alpha, num, preSenderOutput);
            Gf2kSspVoleReceiverThread receiverThread = new Gf2kSspVoleReceiverThread(receiver, delta, num, preReceiverOutput);
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
            // verify
            Gf2kSspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kSspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    @Test
    public void testResetDelta() {
        Gf2kSspVoleSender sender = Gf2kSspVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Gf2kSspVoleReceiver receiver = Gf2kSspVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = GF2K.createRangeRandom(SECURE_RANDOM);
            int alpha = SECURE_RANDOM.nextInt(num);
            // first round
            Gf2kSspVoleSenderThread senderThread = new Gf2kSspVoleSenderThread(sender, alpha, num);
            Gf2kSspVoleReceiverThread receiverThread = new Gf2kSspVoleReceiverThread(receiver, delta, num);
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
            Gf2kSspVoleSenderOutput firstSenderOutput = senderThread.getSenderOutput();
            Gf2kSspVoleReceiverOutput firstReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, firstSenderOutput, firstReceiverOutput);
            // second round, reset Δ
            delta = GF2K.createRangeRandom(SECURE_RANDOM);
            alpha = SECURE_RANDOM.nextInt(num);
            senderThread = new Gf2kSspVoleSenderThread(sender, alpha, num);
            receiverThread = new Gf2kSspVoleReceiverThread(receiver, delta, num);
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
            Gf2kSspVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Gf2kSspVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, secondSenderOutput, secondReceiverOutput);
            // Δ should be unequal
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondReceiverOutput.getDelta()), ByteBuffer.wrap(firstReceiverOutput.getDelta())
            );
            // communication should be equal
            Assert.assertEquals(secondSenderByteLength, firstSenderByteLength);
            Assert.assertEquals(secondReceiverByteLength, firstReceiverByteLength);
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

    private void assertOutput(int num, Gf2kSspVoleSenderOutput senderOutput, Gf2kSspVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        IntStream.range(0, num).forEach(index -> {
            byte[] w = senderOutput.getT(index);
            byte[] v = receiverOutput.getQ(index);
            if (index == senderOutput.getAlpha()) {
                // w = v + Δ · u
                byte[] u = senderOutput.getX();
                byte[] delta = receiverOutput.getDelta();
                byte[] vPrime = BytesUtils.clone(delta);
                GF2K.muli(vPrime, u);
                GF2K.addi(vPrime, v);
                Assert.assertArrayEquals(w, vPrime);
            } else {
                // w = v
                Assert.assertArrayEquals(w, v);
            }
        });
    }
}
