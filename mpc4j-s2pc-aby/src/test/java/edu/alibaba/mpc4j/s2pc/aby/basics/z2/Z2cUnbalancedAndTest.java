package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

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
import java.util.stream.IntStream;

/**
 * Boolean circuit test for unbalanced and operator f · x.
 *
 * @author Feng Han
 * @date 2025/4/16
 */
@RunWith(Parameterized.class)
public class Z2cUnbalancedAndTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2cUnbalancedAndTest.class);
    /**
     * default number of bits
     */
    private static final int DEFAULT_BIT_NUM = 999;
    /**
     * large number of bits
     */
    private static final int LARGE_BIT_NUM = (1 << 16) + 1;
    /**
     * vector length
     */
    private static final int VECTOR_LENGTH = 13;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRG+21
        configurations.add(new Object[]{
            Z2cFactory.BcType.RRG21.name(), new Rrg21Z2cConfig.Builder(true).build()
        });
        // Bea91
        configurations.add(new Object[]{
            Z2cFactory.BcType.BEA91.name(), new Bea91Z2cConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final Z2cConfig config;

    public Z2cUnbalancedAndTest(String name, Z2cConfig config) {
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
        testAnd(bitNum, parallel);
    }

    private void testAnd(int maxBitNum, boolean parallel) {
        Z2cParty sender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Z2cParty receiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate xs
        int bitNum = SECURE_RANDOM.nextInt(maxBitNum) + 1;
        BitVector fVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        BitVector[] xVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> BitVectorFactory.createRandom(bitNum, SECURE_RANDOM))
            .toArray(BitVector[]::new);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            UnbalancedAndZ2cSenderThread senderThread
                = new UnbalancedAndZ2cSenderThread(sender, fVector, xVectors);
            UnbalancedAndZ2cReceiverThread receiverThread
                = new UnbalancedAndZ2cReceiverThread(receiver, fVector, xVectors);
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
            BitVector[] zVectors = senderThread.getExpectVectors();
            // (plain, plain)
            Assert.assertArrayEquals(zVectors, senderThread.getSendPlainPlainVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvPlainPlainVectors());
            // (plain, secret)
            Assert.assertArrayEquals(zVectors, senderThread.getSendPlainSecretVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvPlainSecretVectors());
            // (secret, plain)
            Assert.assertArrayEquals(zVectors, senderThread.getSendSecretPlainVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvSecretPlainVectors());
            // (secret, secret)
            Assert.assertArrayEquals(zVectors, senderThread.getSendSecretSecretVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvSecretSecretVectors());
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
