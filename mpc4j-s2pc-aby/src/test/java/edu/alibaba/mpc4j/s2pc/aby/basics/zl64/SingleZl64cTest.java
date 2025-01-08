package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.Zl64cFactory.Zl64cType;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.bea91.Bea91Zl64cConfig;
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
 * single Zl circuit test.
 *
 * @author Li Peng
 * @date 2024/7/25
 */
@RunWith(Parameterized.class)
public class SingleZl64cTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleZl64cTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1001;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 14) - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Bea91
        configurations.add(new Object[]{
            Zl64cType.BEA91.name() + ", " + SecurityModel.SEMI_HONEST + ")",
            new Bea91Zl64cConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final Zl64cConfig config;
    /**
     * Zl instance
     */
    private final Zl64 zl64;

    public SingleZl64cTest(String name, Zl64cConfig config) {
        super(name);
        this.config = config;
        zl64 = Zl64Factory.createInstance(EnvType.STANDARD, LongUtils.MAX_L_FOR_MODULE_N - 1);
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
    public void test8Num() {
        testPto(8, false);
    }

    @Test
    public void test15Num() {
        testPto(15, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        for (DyadicAcOperator operator : DyadicAcOperator.values()) {
            testDyadicOperator(operator, num, parallel);
        }
        for (UnaryAcOperator operator : UnaryAcOperator.values()) {
            testUnaryOperator(operator, num, parallel);
        }
    }

    private void testDyadicOperator(DyadicAcOperator operator, int num, boolean parallel) {
        Zl64cParty sender = Zl64cFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Zl64cParty receiver = Zl64cFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        Zl64Vector xVector = Zl64Vector.createRandom(zl64, num, SECURE_RANDOM);
        // generate y
        Zl64Vector yVector = Zl64Vector.createRandom(zl64, num, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleDyadicZl64cSenderThread senderThread = new SingleDyadicZl64cSenderThread(sender, zl64, operator, xVector, yVector);
            SingleDyadicZl64cReceiverThread receiverThread = new SingleDyadicZl64cReceiverThread(receiver, zl64, operator, xVector, yVector);
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
            Zl64Vector zVector = senderThread.getExpectVector();
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
    private void testUnaryOperator(UnaryAcOperator operator, int num, boolean parallel) {
        Zl64cParty sender = Zl64cFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Zl64cParty receiver = Zl64cFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        Zl64Vector xVector = Zl64Vector.createRandom(zl64, num, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleUnaryZl64cSenderThread senderThread = new SingleUnaryZl64cSenderThread(sender, zl64, operator, xVector);
            SingleUnaryZl64cReceiverThread receiverThread = new SingleUnaryZl64cReceiverThread(receiver, zl64, operator, xVector);
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
            Zl64Vector zVector = senderThread.getExpectVector();
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
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), operator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
