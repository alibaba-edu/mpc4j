package edu.alibaba.mpc4j.s2pc.aby.basics.bc.vector;

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
import org.junit.Assert;
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
 * Vector Boolean circuit protocol test.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
@RunWith(Parameterized.class)
public class BcVectorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BcVectorTest.class);
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
     * default vector lengtg
     */
    private static final int MAX_VECTOR_LENGTH = 13;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // Beaver91
        configurations.add(new Object[]{
            BcFactory.BcType.BEA91.name(), new Bea91BcConfig.Builder().build()
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

    public BcVectorTest(String name, BcConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1BitNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2BitNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void test8BitNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 8);
    }

    @Test
    public void test15BitNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 8);
    }

    @Test
    public void testDefaultBitNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_BIT_NUM);
    }

    @Test
    public void testParallelDefaultBitNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_BIT_NUM);
    }

    @Test
    public void testLargeBitNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_BIT_NUM);
    }

    @Test
    public void testParallelLargeBitNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_BIT_NUM);
    }

    private void testPto(BcParty sender, BcParty receiver, int bitNum) {
        testBinaryOperator(sender, receiver, BcOperator.XOR, bitNum);
        testBinaryOperator(sender, receiver, BcOperator.AND, bitNum);
        testBinaryOperator(sender, receiver, BcOperator.OR, bitNum);
        testUnaryOperator(sender, receiver, BcOperator.NOT, bitNum);
    }

    private void testBinaryOperator(BcParty sender, BcParty receiver, BcOperator bcOperator, int maxBitNum) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
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
            BcVectorBinarySenderThread senderThread
                = new BcVectorBinarySenderThread(sender, bcOperator, xBitVectors, yBitVectors);
            BcVectorBinaryReceiverThread receiverThread
                = new BcVectorBinaryReceiverThread(receiver, bcOperator, xBitVectors, yBitVectors);
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
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
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
            BcVectorUnarySenderThread senderThread
                = new BcVectorUnarySenderThread(sender, bcOperator, xBitVectors);
            BcVectorUnaryReceiverThread receiverThread
                = new BcVectorUnaryReceiverThread(receiver, bcOperator, xBitVectors);
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
