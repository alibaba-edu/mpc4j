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
import java.util.stream.IntStream;

/**
 * batch Zl circuit test.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
@RunWith(Parameterized.class)
public class BatchZlcTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchZlcTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 999;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 12) + 1;
    /**
     * vector length
     */
    private static final int VECTOR_LENGTH = 13;

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

    public BatchZlcTest(String name, ZlcConfig config) {
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
        testPto(15, true);
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

    private void testDyadicOperator(DyadicAcOperator operator, int maxNum, boolean parallel) {
        ZlcParty sender = ZlcFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlcParty receiver = ZlcFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate xs
        ZlVector[] xVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> {
                // sample num in [1, maxNum]
                int num = SECURE_RANDOM.nextInt(maxNum) + 1;
                return ZlVector.createRandom(zl, num, SECURE_RANDOM);
            })
            .toArray(ZlVector[]::new);
        // generate ys
        ZlVector[] yVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> {
                int num = xVectors[index].getNum();
                return ZlVector.createRandom(zl, num, SECURE_RANDOM);
            })
            .toArray(ZlVector[]::new);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            BatchDyadicZlcSenderThread senderThread
                = new BatchDyadicZlcSenderThread(sender, operator, xVectors, yVectors);
            BatchDyadicZlcReceiverThread receiverThread
                = new BatchDyadicZlcReceiverThread(receiver, operator, xVectors, yVectors);
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
            ZlVector[] expectVectors = senderThread.getExpectVectors();
            // (plain, plain)
            Assert.assertArrayEquals(expectVectors, senderThread.getSendPlainPlainVectors());
            Assert.assertArrayEquals(expectVectors, receiverThread.getRecvPlainPlainVectors());
            // (plain, secret)
            Assert.assertArrayEquals(expectVectors, senderThread.getSendPlainSecretVectors());
            Assert.assertArrayEquals(expectVectors, receiverThread.getRecvPlainSecretVectors());
            // (secret, plain)
            Assert.assertArrayEquals(expectVectors, senderThread.getSendSecretPlainVectors());
            Assert.assertArrayEquals(expectVectors, receiverThread.getRecvSecretPlainVectors());
            // (secret, secret)
            Assert.assertArrayEquals(expectVectors, senderThread.getSendSecretSecretVectors());
            Assert.assertArrayEquals(expectVectors, receiverThread.getRecvSecretSecretVectors());
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
    private void testUnaryOperator(UnaryAcOperator operator, int maxNum, boolean parallel) {
        ZlcParty sender = ZlcFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlcParty receiver = ZlcFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate xs
        ZlVector[] xVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> {
                // sample bitNum in [1, maxBitNum]
                int num = SECURE_RANDOM.nextInt(maxNum) + 1;
                return ZlVector.createRandom(zl, num, SECURE_RANDOM);
            })
            .toArray(ZlVector[]::new);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            BatchUnaryZlcSenderThread senderThread = new BatchUnaryZlcSenderThread(sender, operator, xVectors);
            BatchUnaryZlcReceiverThread receiverThread = new BatchUnaryZlcReceiverThread(receiver, operator, xVectors);
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
            ZlVector[] zVectors = senderThread.getExpectVectors();
            // (plain)
            Assert.assertArrayEquals(zVectors, senderThread.getSendPlainVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvPlainVectors());
            // (secret)
            Assert.assertArrayEquals(zVectors, senderThread.getSendSecretVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvSecretVectors());
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
