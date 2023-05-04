package edu.alibaba.mpc4j.s2pc.aby.basics.bc.batch;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91.Bea91BcConfig;
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
 * batch Boolean circuit test.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
@RunWith(Parameterized.class)
public class BatchBcTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchBcTest.class);
    /**
     * random status
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default number of bits
     */
    private static final int DEFAULT_BIT_NUM = 1000;
    /**
     * large number of bits
     */
    private static final int LARGE_BIT_NUM = 1 << 16;
    /**
     * default vector length
     */
    private static final int MAX_VECTOR_LENGTH = 13;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRG+21
        configurations.add(new Object[]{
            BcFactory.BcType.RRG21.name(), new Bea91BcConfig.Builder().build()
        });
        // Bea91
        configurations.add(new Object[]{
            BcFactory.BcType.Bea91.name(), new Bea91BcConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * 发送方
     */
    private final Rpc senderRpc;
    /**
     * 接收方
     */
    private final Rpc receiverRpc;
    /**
     * 协议类型
     */
    private final BcConfig config;

    public BatchBcTest(String name, BcConfig config) {
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
    public void test1BitNum() {
        testPto(1, false);
    }

    @Test
    public void test2BitNum() {
        testPto(2, false);
    }

    @Test
    public void test8BitNum() {
        testPto(8, false);
    }

    @Test
    public void test15BitNum() {
        testPto(15, true);
    }

    @Test
    public void testDefaultBitNum() {
        testPto(DEFAULT_BIT_NUM, false);
    }

    @Test
    public void testParallelDefaultBitNum() {
        testPto(DEFAULT_BIT_NUM, true);
    }

    @Test
    public void testLargeBitNum() {
        testPto(LARGE_BIT_NUM, false);
    }

    @Test
    public void testParallelLargeBitNum() {
        testPto(LARGE_BIT_NUM, true);
    }

    private void testPto(int bitNum, boolean parallel) {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        testDyadicOperator(sender, receiver, BcOperator.XOR, bitNum);
        testDyadicOperator(sender, receiver, BcOperator.AND, bitNum);
        testDyadicOperator(sender, receiver, BcOperator.OR, bitNum);
        testUnaryOperator(sender, receiver, BcOperator.NOT, bitNum);
        sender.destroy();
        receiver.destroy();
    }

    private void testDyadicOperator(BcParty sender, BcParty receiver, BcOperator bcOperator, int maxBitNum) {
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate xs
        BitVector[] xBitVectors = IntStream.range(0, MAX_VECTOR_LENGTH)
            .mapToObj(index -> {
                // sample bitNum in [1, maxBitNum]
                int bitNum = SECURE_RANDOM.nextInt(maxBitNum) + 1;
                return BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
            })
            .toArray(BitVector[]::new);
        // generate ys
        BitVector[] yBitVectors = IntStream.range(0, MAX_VECTOR_LENGTH)
            .mapToObj(index -> {
                int bitNum = xBitVectors[index].bitNum();
                return BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
            })
            .toArray(BitVector[]::new);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
            BatchDyadicBcSenderThread senderThread
                = new BatchDyadicBcSenderThread(sender, bcOperator, xBitVectors, yBitVectors);
            BatchDyadicBcReceiverThread receiverThread
                = new BatchDyadicBcReceiverThread(receiver, bcOperator, xBitVectors, yBitVectors);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
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
            BitVector[] expectBitVectors = senderThread.getExpectVectors();
            // (plain, plain)
            Assert.assertArrayEquals(expectBitVectors, senderThread.getZ11Vectors());
            Assert.assertArrayEquals(expectBitVectors, receiverThread.getZ11Vectors());
            // (plain, secret)
            Assert.assertArrayEquals(expectBitVectors, senderThread.getZ10Vectors());
            Assert.assertArrayEquals(expectBitVectors, receiverThread.getZ10Vectors());
            // (secret, plain)
            Assert.assertArrayEquals(expectBitVectors, senderThread.getZ01Vectors());
            Assert.assertArrayEquals(expectBitVectors, receiverThread.getZ01Vectors());
            // (secret, secret)
            Assert.assertArrayEquals(expectBitVectors, senderThread.getZ00Vectors());
            Assert.assertArrayEquals(expectBitVectors, receiverThread.getZ00Vectors());
            // immutable shares
            Assert.assertArrayEquals(senderThread.getShareX0s(), senderThread.getFinalX010s());
            Assert.assertArrayEquals(senderThread.getShareX0s(), senderThread.getFinalX000s());
            Assert.assertArrayEquals(receiverThread.getShareX1s(), receiverThread.getFinalX011s());
            Assert.assertArrayEquals(receiverThread.getShareX1s(), receiverThread.getFinalX001s());

            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(BcParty sender, BcParty receiver, BcOperator bcOperator, int maxBitNum) {
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate xs
        BitVector[] xBitVectors = IntStream.range(0, MAX_VECTOR_LENGTH)
            .mapToObj(index -> {
                // sample bitNum in [1, maxBitNum]
                int bitNum = SECURE_RANDOM.nextInt(maxBitNum) + 1;
                return BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
            })
            .toArray(BitVector[]::new);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
            BatchUnaryBcSenderThread senderThread
                = new BatchUnaryBcSenderThread(sender, bcOperator, xBitVectors);
            BatchUnaryBcReceiverThread receiverThread
                = new BatchUnaryBcReceiverThread(receiver, bcOperator, xBitVectors);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
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
            BitVector[] expectBitVectors = senderThread.getExpectBitVectors();
            // (plain)
            Assert.assertArrayEquals(expectBitVectors, senderThread.getZ1Vectors());
            Assert.assertArrayEquals(expectBitVectors, receiverThread.getZ1Vectors());
            // (secret)
            Assert.assertArrayEquals(expectBitVectors, senderThread.getZ0Vectors());
            Assert.assertArrayEquals(expectBitVectors, receiverThread.getZ0Vectors());
            // immutable shares
            Assert.assertArrayEquals(senderThread.getShareX0s(), senderThread.getFinalX0s());
            Assert.assertArrayEquals(receiverThread.getShareX1s(), receiverThread.getFinalX1s());

            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
