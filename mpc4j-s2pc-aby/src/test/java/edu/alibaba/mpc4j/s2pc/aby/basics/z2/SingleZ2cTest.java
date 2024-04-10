package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicBcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryBcOperator;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21.Rrg21Z2cConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SingleZ2cTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleZ2cTest.class);
    /**
     * default number of bits
     */
    private static final int DEFAULT_BIT_NUM = 1001;
    /**
     * large number of bits
     */
    private static final int LARGE_BIT_NUM = (1 << 18) - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRG+21
        configurations.add(new Object[] {
            Z2cFactory.BcType.RRG21.name(), new Rrg21Z2cConfig.Builder().build()
        });
        // Bea91
        configurations.add(new Object[] {
            Z2cFactory.BcType.BEA91.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new Bea91Z2cConfig.Builder(SecurityModel.SEMI_HONEST).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final Z2cConfig config;

    public SingleZ2cTest(String name, Z2cConfig config) {
        super(name);
        this.config = config;
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
        for (DyadicBcOperator operator : DyadicBcOperator.values()) {
            testDyadicOperator(operator, bitNum, parallel);
        }
        for (UnaryBcOperator operator : UnaryBcOperator.values()) {
            testUnaryOperator(operator, bitNum, parallel);
        }
    }

    private void testDyadicOperator(DyadicBcOperator operator, int bitNum, boolean parallel) {
        Z2cParty sender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Z2cParty receiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        BitVector xVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        // generate y
        BitVector yVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleDyadicZ2cSenderThread senderThread = new SingleDyadicZ2cSenderThread(sender, operator, xVector, yVector);
            SingleDyadicZ2cReceiverThread receiverThread = new SingleDyadicZ2cReceiverThread(receiver, operator, xVector, yVector);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            BitVector zVector = senderThread.getExpectVector();
            // (plain, plain)
            Assert.assertEquals(zVector, senderThread.getSenPlainPlainVector());
            Assert.assertEquals(zVector, receiverThread.getRecvPlainPlainVector());
            // (plain, secret)
            Assert.assertEquals(zVector, senderThread.getSendPlainSecretVector());
            Assert.assertEquals(zVector, receiverThread.getRecvPlainSecretVector());
            // (secret, plain)
            Assert.assertEquals(zVector, senderThread.getSendSecretPlainVector());
            Assert.assertEquals(zVector, receiverThread.getRecvSecretPlainVector());
            // (secret, secret)
            Assert.assertEquals(zVector, senderThread.getSendSecretSecretVector());
            Assert.assertEquals(zVector, receiverThread.getRecvSecretSecretVector());
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), operator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(UnaryBcOperator operator, int bitNum, boolean parallel) {
        Z2cParty sender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Z2cParty receiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        BitVector xVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleUnaryZ2cSenderThread senderThread = new SingleUnaryZ2cSenderThread(sender, operator, xVector);
            SingleUnaryZ2cReceiverThread receiverThread = new SingleUnaryZ2cReceiverThread(receiver, operator, xVector);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            BitVector zVector = senderThread.getExpectVector();
            // (plain)
            Assert.assertEquals(zVector, senderThread.getSendPlainVector());
            Assert.assertEquals(zVector, receiverThread.getRecvPlainVector());
            // (secret)
            Assert.assertEquals(zVector, senderThread.getSendSecretVector());
            Assert.assertEquals(zVector, receiverThread.getRecvSecretVector());
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
