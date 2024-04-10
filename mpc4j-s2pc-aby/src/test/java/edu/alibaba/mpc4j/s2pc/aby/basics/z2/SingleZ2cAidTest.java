package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicBcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryBcOperator;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cConfig;
import edu.alibaba.mpc4j.s2pc.pcg.aid.AiderThread;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealAider;
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
 * single Boolean circuit aid test.
 *
 * @author Weiran Liu
 * @date 2023/6/26
 */
@RunWith(Parameterized.class)
public class SingleZ2cAidTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleZ2cAidTest.class);
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

        // Bea91
        configurations.add(new Object[] {
            Z2cFactory.BcType.BEA91.name() + " (" + SecurityModel.TRUSTED_DEALER + ")",
            new Bea91Z2cConfig.Builder(SecurityModel.TRUSTED_DEALER).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final Z2cConfig config;

    public SingleZ2cAidTest(String name, Z2cConfig config) {
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
        Z2cParty sender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        Z2cParty receiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
        TrustDealAider aider = new TrustDealAider(thirdRpc, firstRpc.ownParty(), secondRpc.ownParty());
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        aider.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        aider.setTaskId(randomTaskId);
        // generate x
        BitVector xVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        // generate y
        BitVector yVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleDyadicZ2cSenderThread senderThread = new SingleDyadicZ2cSenderThread(sender, operator, xVector, yVector);
            SingleDyadicZ2cReceiverThread receiverThread = new SingleDyadicZ2cReceiverThread(receiver, operator, xVector, yVector);
            AiderThread aiderThread = new AiderThread(aider);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            aiderThread.start();
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
            aiderThread.join();
            new Thread(aider::destroy).start();
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), operator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(UnaryBcOperator operator, int bitNum, boolean parallel) {
        Z2cParty sender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        Z2cParty receiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
        TrustDealAider aider = new TrustDealAider(thirdRpc, firstRpc.ownParty(), secondRpc.ownParty());
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        aider.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        aider.setTaskId(randomTaskId);
        // generate x
        BitVector xVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleUnaryZ2cSenderThread senderThread = new SingleUnaryZ2cSenderThread(sender, operator, xVector);
            SingleUnaryZ2cReceiverThread receiverThread = new SingleUnaryZ2cReceiverThread(receiver, operator, xVector);
            AiderThread aiderThread = new AiderThread(aider);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            aiderThread.start();
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
            aiderThread.join();
            new Thread(aider::destroy).start();
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), operator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
