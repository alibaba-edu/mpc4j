package edu.alibaba.mpc4j.s2pc.aby.basics.bc.operator;

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

/**
 * Boolean circuit protocol test.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
@RunWith(Parameterized.class)
public class BcOperatorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BcOperatorTest.class);
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
    private static final int LARGE_BIT_NUM = 1 << 18;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // Beaver91
        configurations.add(new Object[] {
            BcFactory.BcType.BEA91.name(), new Bea91BcConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * the sender
     */
    private final Rpc senderRpc;
    /**
     * the receiver
     */
    private final Rpc receiverRpc;
    /**
     * the protocol configuration
     */
    private final BcConfig config;

    public BcOperatorTest(String name, BcConfig config) {
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

    private void testBinaryOperator(BcParty sender, BcParty receiver, BcOperator bcOperator, int bitNum) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        BitVector xBitVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        // generate y
        BitVector yBitVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
            BcBinarySenderThread senderThread = new BcBinarySenderThread(sender, bcOperator, xBitVector, yBitVector);
            BcBinaryReceiverThread receiverThread = new BcBinaryReceiverThread(receiver, bcOperator, xBitVector, yBitVector);
            StopWatch stopWatch = new StopWatch();

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
            BitVector expectBitVector = senderThread.getExpectVector();
            // (plain, plain)
            Assert.assertEquals(expectBitVector, senderThread.getZ11Vector());
            Assert.assertEquals(expectBitVector, receiverThread.getZ11Vector());
            // (plain, secret)
            Assert.assertEquals(expectBitVector, senderThread.getZ10Vector());
            Assert.assertEquals(expectBitVector, receiverThread.getZ10Vector());
            // (secret, plain)
            Assert.assertEquals(expectBitVector, senderThread.getZ01Vector());
            Assert.assertEquals(expectBitVector, receiverThread.getZ01Vector());
            // (secret, secret)
            Assert.assertEquals(expectBitVector, senderThread.getZ00Vector());
            Assert.assertEquals(expectBitVector, receiverThread.getZ00Vector());
            // immutable shares
            Assert.assertEquals(senderThread.getShareX0(), senderThread.getFinalX010());
            Assert.assertEquals(senderThread.getShareX0(), senderThread.getFinalX000());
            Assert.assertEquals(receiverThread.getShareX1(), receiverThread.getFinalX011());
            Assert.assertEquals(receiverThread.getShareX1(), receiverThread.getFinalX001());

            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(BcParty sender, BcParty receiver, BcOperator bcOperator, int bitNum) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        BitVector xBitVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
            BcUnarySenderThread senderThread = new BcUnarySenderThread(sender, bcOperator, xBitVector);
            BcUnaryReceiverThread receiverThread = new BcUnaryReceiverThread(receiver, bcOperator, xBitVector);
            StopWatch stopWatch = new StopWatch();

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
            BitVector expectBitVector = senderThread.getExpectVector();
            // (plain)
            Assert.assertEquals(expectBitVector, senderThread.getZ1Vector());
            Assert.assertEquals(expectBitVector, receiverThread.getZ1Vector());
            // (secret)
            Assert.assertEquals(expectBitVector, senderThread.getZ0Vector());
            Assert.assertEquals(expectBitVector, receiverThread.getZ0Vector());
            // immutable shares
            Assert.assertEquals(senderThread.getShareX0(), senderThread.getFinalX0());
            Assert.assertEquals(receiverThread.getShareX1(), receiverThread.getFinalX1());

            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
