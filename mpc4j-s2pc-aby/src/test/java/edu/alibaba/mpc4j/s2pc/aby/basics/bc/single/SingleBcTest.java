package edu.alibaba.mpc4j.s2pc.aby.basics.bc.single;

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
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.rrg21.Rrg21BcConfig;
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

/**
 * single Boolean circuit test.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
@RunWith(Parameterized.class)
public class SingleBcTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleBcTest.class);
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

        // RRG+21
        configurations.add(new Object[] {
            BcFactory.BcType.RRG21.name(), new Rrg21BcConfig.Builder().build()
        });
        // Bea91
        configurations.add(new Object[] {
            BcFactory.BcType.Bea91.name(), new Bea91BcConfig.Builder().build()
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

    public SingleBcTest(String name, BcConfig config) {
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
        testPto(15, false);
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

    private void testDyadicOperator(BcParty sender, BcParty receiver, BcOperator bcOperator, int bitNum) {
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        BitVector xBitVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        // generate y
        BitVector yBitVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
            SingleDyadicBcSenderThread senderThread = new SingleDyadicBcSenderThread(sender, bcOperator, xBitVector, yBitVector);
            SingleDyadicBcReceiverThread receiverThread = new SingleDyadicBcReceiverThread(receiver, bcOperator, xBitVector, yBitVector);
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
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        BitVector xBitVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), bcOperator.name());
            SingleUnaryBcSenderThread senderThread = new SingleUnaryBcSenderThread(sender, bcOperator, xBitVector);
            SingleUnaryBcReceiverThread receiverThread = new SingleUnaryBcReceiverThread(receiver, bcOperator, xBitVector);
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
