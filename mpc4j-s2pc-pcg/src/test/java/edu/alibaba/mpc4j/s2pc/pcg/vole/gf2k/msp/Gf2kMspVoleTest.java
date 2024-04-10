package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleFactory.Gf2kMspVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19.Bcg19RegGf2kMspVoleConfig;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
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
 * GF2K-MSP-VOLE tests.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
@RunWith(Parameterized.class)
public class Gf2kMspVoleTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kMspVoleTest.class);
    /**
     * GF2K
     */
    private static final Gf2k GF2K = Gf2kFactory.createInstance(EnvType.STANDARD);
    /**
     * default sparse num
     */
    private static final int DEFAULT_T = 1 << 4;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1 << 10;
    /**
     * large spare num
     */
    private static final int LARGE_T = 1 << 10;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BCG19_REG
        configurations.add(new Object[] {
            Gf2kMspVoleType.BCG19_REG.name() + " (" + SecurityModel.MALICIOUS.name() + ")",
            new Bcg19RegGf2kMspVoleConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[] {
            Gf2kMspVoleType.BCG19_REG.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Bcg19RegGf2kMspVoleConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kMspVoleConfig config;

    public Gf2kMspVoleTest(String name, Gf2kMspVoleConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testDefaultNum1T() {
        testPto(1, DEFAULT_NUM, false);
    }

    @Test
    public void testDefaultNum2T() {
        testPto(2, DEFAULT_NUM, false);
    }

    @Test
    public void test1Num1T() {
        testPto(1, 1, false);
    }

    @Test
    public void test2Num2T() {
        testPto(2, 2, false);
    }

    @Test
    public void testDefaultNumDefaultT() {
        testPto(DEFAULT_T, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNumDefaultT() {
        testPto(DEFAULT_T, DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNumLargeT() {
        testPto(LARGE_T, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNumLargeT() {
        testPto(LARGE_T, LARGE_NUM, true);
    }

    private void testPto(int t, int num, boolean parallel) {
        Gf2kMspVoleSender sender = Gf2kMspVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kMspVoleReceiver receiver = Gf2kMspVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            Gf2kMspVoleSenderThread senderThread = new Gf2kMspVoleSenderThread(sender, t, num);
            Gf2kMspVoleReceiverThread receiverThread = new Gf2kMspVoleReceiverThread(receiver, delta, t, num);
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            Gf2kMspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kMspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
//
//    @Test
//    public void testPrecomputeLargeNumLargeT() {
//        MspCotSender sender = MspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
//        MspCotReceiver receiver = MspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
//        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
//        sender.setTaskId(randomTaskId);
//        receiver.setTaskId(randomTaskId);
//        try {
//            int num = LARGE_NUM;
//            int t = LARGE_T;
//            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
//            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
//            SECURE_RANDOM.nextBytes(delta);
//            CotSenderOutput preSenderOutput = CotTestUtils.genSenderOutput(
//                MspCotFactory.getPrecomputeNum(config, t, num), delta, SECURE_RANDOM
//            );
//            CotReceiverOutput preReceiverOutput = CotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
//            MspCotSenderThread senderThread = new MspCotSenderThread(sender, delta, t, num, preSenderOutput);
//            MspCotReceiverThread receiverThread = new MspCotReceiverThread(receiver, t, num, preReceiverOutput);
//            STOP_WATCH.start();
//            // start
//            senderThread.start();
//            receiverThread.start();
//            // stop
//            senderThread.join();
//            receiverThread.join();
//            STOP_WATCH.stop();
//            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
//            STOP_WATCH.reset();
//            // verify
//            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
//            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
//            assertOutput(num, senderOutput, receiverOutput);
//            printAndResetRpc(time);
//            // destroy
//            new Thread(sender::destroy).start();
//            new Thread(receiver::destroy).start();
//            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testResetDelta() {
//        MspCotSender sender = MspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
//        MspCotReceiver receiver = MspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
//        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
//        sender.setTaskId(randomTaskId);
//        receiver.setTaskId(randomTaskId);
//        try {
//            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
//            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
//            SECURE_RANDOM.nextBytes(delta);
//            // first round
//            MspCotSenderThread senderThread = new MspCotSenderThread(sender, delta, DEFAULT_T, DEFAULT_NUM);
//            MspCotReceiverThread receiverThread = new MspCotReceiverThread(receiver, DEFAULT_T, DEFAULT_NUM);
//            STOP_WATCH.start();
//            senderThread.start();
//            receiverThread.start();
//            senderThread.join();
//            receiverThread.join();
//            STOP_WATCH.stop();
//            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
//            STOP_WATCH.reset();
//            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
//            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
//            assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
//            printAndResetRpc(firstTime);
//            // second round, reset Δ
//            SECURE_RANDOM.nextBytes(delta);
//            senderThread = new MspCotSenderThread(sender, delta, DEFAULT_T, DEFAULT_NUM);
//            receiverThread = new MspCotReceiverThread(receiver, DEFAULT_T, DEFAULT_NUM);
//            STOP_WATCH.start();
//            senderThread.start();
//            receiverThread.start();
//            senderThread.join();
//            receiverThread.join();
//            STOP_WATCH.stop();
//            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
//            STOP_WATCH.reset();
//            MspCotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
//            MspCotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
//            assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
//            // Δ should be different
//            Assert.assertNotEquals(
//                ByteBuffer.wrap(secondSenderOutput.getDelta()), ByteBuffer.wrap(senderOutput.getDelta())
//            );
//            printAndResetRpc(secondTime);
//            // destroy
//            new Thread(sender::destroy).start();
//            new Thread(receiver::destroy).start();
//            LOGGER.info("-----test {} (reset Δ) end-----", sender.getPtoDesc().getPtoName());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    private void assertOutput(int num, Gf2kMspVoleSenderOutput senderOutput, Gf2kMspVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        TIntSet alphaSet = new TIntHashSet(senderOutput.getAlphaArray());
        IntStream.range(0, num).forEach(index -> {
            // w = v + Δ · u
            byte[] w = senderOutput.getT(index);
            byte[] v = receiverOutput.getQ(index);
            byte[] u = senderOutput.getX(index);
            byte[] delta = receiverOutput.getDelta();
            byte[] vPrime = BytesUtils.clone(delta);
            GF2K.muli(vPrime, u);
            GF2K.addi(vPrime, v);
            Assert.assertArrayEquals(w, vPrime);
            if (alphaSet.contains(index)) {
                // u is non-zero
                Assert.assertFalse(GF2K.isZero(u));
            } else {
                // u is zero
                Assert.assertTrue(GF2K.isZero(u));
            }
        });
    }
}
