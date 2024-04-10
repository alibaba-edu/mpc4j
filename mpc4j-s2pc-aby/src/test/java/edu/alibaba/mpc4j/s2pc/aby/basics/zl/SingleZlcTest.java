package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.bea91.Bea91ZlcConfig;
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
 * @author Weiran Liu
 * @date 2023/5/11
 */
@RunWith(Parameterized.class)
public class SingleZlcTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleZlcTest.class);
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

        Zl[] zls = new Zl[]{
            ZlFactory.createInstance(EnvType.STANDARD, 1),
            ZlFactory.createInstance(EnvType.STANDARD, 3),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L - 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L + 1),
        };

        for (Zl zl : zls) {
            int l = zl.getL();
            // Bea91
            configurations.add(new Object[]{
                ZlcFactory.ZlType.BEA91.name() + " (l = " + l + ", " + SecurityModel.SEMI_HONEST + ")",
                new Bea91ZlcConfig.Builder(SecurityModel.SEMI_HONEST, zl).build()
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final ZlcConfig config;
    /**
     * Zl instance
     */
    private final Zl zl;

    public SingleZlcTest(String name, ZlcConfig config) {
        super(name);
        this.config = config;
        zl = config.getZl();
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
        ZlcParty sender = ZlcFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlcParty receiver = ZlcFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        ZlVector xVector = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        // generate y
        ZlVector yVector = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleDyadicZlcSenderThread senderThread = new SingleDyadicZlcSenderThread(sender, operator, xVector, yVector);
            SingleDyadicZlcReceiverThread receiverThread = new SingleDyadicZlcReceiverThread(receiver, operator, xVector, yVector);
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
            ZlVector zVector = senderThread.getExpectVector();
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
        ZlcParty sender = ZlcFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlcParty receiver = ZlcFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        ZlVector xVector = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleUnaryZlcSenderThread senderThread = new SingleUnaryZlcSenderThread(sender, operator, xVector);
            SingleUnaryZlcReceiverThread receiverThread = new SingleUnaryZlcReceiverThread(receiver, operator, xVector);
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
            ZlVector zVector = senderThread.getExpectVector();
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
