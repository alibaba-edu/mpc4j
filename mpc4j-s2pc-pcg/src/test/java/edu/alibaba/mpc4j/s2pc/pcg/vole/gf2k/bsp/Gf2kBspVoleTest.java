package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory.Gf2kBspVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21.Wykw21MaGf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21.Wykw21ShGf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleSenderOutput;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Batched single-point GF2K-VOLE tests.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
@RunWith(Parameterized.class)
public class Gf2kBspVoleTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kBspVoleTest.class);
    /**
     * GF2K
     */
    private static final Gf2k GF2K = Gf2kFactory.createInstance(EnvType.STANDARD);
    /**
     * default num, the num is not even, and not in format 2^k
     */
    private static final int DEFAULT_EACH_NUM = 9;
    /**
     * large num
     */
    private static final int LARGE_EACH_NUM = 1 << 16;
    /**
     * default batch num,  we select an odd number and not with the format 2^k
     */
    private static final int DEFAULT_BATCH_NUM = 9;
    /**
     * large batch num
     */
    private static final int LARGE_BATCH_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // WYKW21_MALICIOUS
        configurations.add(new Object[]{
            Gf2kBspVoleType.WYKW21_MALICIOUS.name(), new Wykw21MaGf2kBspVoleConfig.Builder().build(),
        });
        // WYKW21_SEMI_HONEST
        configurations.add(new Object[]{
            Gf2kBspVoleType.WYKW21_SEMI_HONEST.name(), new Wykw21ShGf2kBspVoleConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kBspVoleConfig config;

    public Gf2kBspVoleTest(String name, Gf2kBspVoleConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testFirstAlpha() {
        //noinspection UnnecessaryLocalVariable
        int num = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> 0)
            .toArray();
        testPto(alphaArray, num, false);
    }

    @Test
    public void testLastAlpha() {
        //noinspection UnnecessaryLocalVariable
        int num = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> DEFAULT_EACH_NUM - 1)
            .toArray();
        testPto(alphaArray, num, false);
    }

    @Test
    public void test1EachNum() {
        int eachNum = 1;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test2EachNum() {
        int eachNum = 2;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test1BatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = 1;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test2BatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = 2;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testDefault() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testParallelDefault() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, true);
    }

    @Test
    public void testLargeBatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = LARGE_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testParallelLargeBatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = LARGE_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, true);
    }

    @Test
    public void testLargeEachNum() {
        int eachNum = LARGE_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testParallelLargeEachNum() {
        int eachNum = LARGE_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, true);
    }

    private void testPto(int[] alphaArray, int num, boolean parallel) {
        Gf2kBspVoleSender sender = Gf2kBspVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kBspVoleReceiver receiver = Gf2kBspVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = GF2K.createRangeRandom(SECURE_RANDOM);
            int batchNum = alphaArray.length;
            Gf2kBspVoleSenderThread senderThread = new Gf2kBspVoleSenderThread(sender, alphaArray, num);
            Gf2kBspVoleReceiverThread receiverThread = new Gf2kBspVoleReceiverThread(receiver, delta, batchNum, num);
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
            Gf2kBspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kBspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPrecompute() {
        Gf2kBspVoleSender sender = Gf2kBspVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kBspVoleReceiver receiver = Gf2kBspVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = DEFAULT_BATCH_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = GF2K.createRangeRandom(SECURE_RANDOM);
            int[] alphaArray = IntStream.range(0, batchNum)
                .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
                .toArray();
            Gf2kVoleReceiverOutput preReceiverOutput = Gf2kVoleTestUtils.genReceiverOutput(
                Gf2kBspVoleFactory.getPrecomputeNum(config, batchNum, eachNum), delta, SECURE_RANDOM
            );
            Gf2kVoleSenderOutput preSenderOutput = Gf2kVoleTestUtils.genSenderOutput(preReceiverOutput, SECURE_RANDOM);
            Gf2kBspVoleSenderThread senderThread = new Gf2kBspVoleSenderThread(
                sender, alphaArray, eachNum, preSenderOutput
            );
            Gf2kBspVoleReceiverThread receiverThread = new Gf2kBspVoleReceiverThread(
                receiver, delta, batchNum, eachNum, preReceiverOutput
            );
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kBspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kBspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, eachNum, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResetDelta() {
        Gf2kBspVoleSender sender = Gf2kBspVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kBspVoleReceiver receiver = Gf2kBspVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = DEFAULT_BATCH_NUM;
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = GF2K.createRangeRandom(SECURE_RANDOM);
            int[] alphaArray = IntStream.range(0, batchNum)
                .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
                .toArray();
            // first round
            Gf2kBspVoleSenderThread senderThread = new Gf2kBspVoleSenderThread(sender, alphaArray, eachNum);
            Gf2kBspVoleReceiverThread receiverThread = new Gf2kBspVoleReceiverThread(receiver, delta, batchNum, eachNum);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kBspVoleSenderOutput firstSenderOutput = senderThread.getSenderOutput();
            Gf2kBspVoleReceiverOutput firstReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, eachNum, firstSenderOutput, firstReceiverOutput);
            printAndResetRpc(firstTime);
            // second round, reset Δ
            delta = GF2K.createRangeRandom(SECURE_RANDOM);
            alphaArray = IntStream.range(0, batchNum)
                .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
                .toArray();
            senderThread = new Gf2kBspVoleSenderThread(sender, alphaArray, eachNum);
            receiverThread = new Gf2kBspVoleReceiverThread(receiver, delta, batchNum, eachNum);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kBspVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Gf2kBspVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, eachNum, secondSenderOutput, secondReceiverOutput);
            // Δ should be different
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondReceiverOutput.getDelta()), ByteBuffer.wrap(firstReceiverOutput.getDelta())
            );
            printAndResetRpc(secondTime);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (reset Δ) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int batchNum, int eachNum,
                              Gf2kBspVoleSenderOutput senderOutput, Gf2kBspVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(batchNum, senderOutput.getBatchNum());
        Assert.assertEquals(batchNum, receiverOutput.getBatchNum());
        Assert.assertEquals(eachNum, senderOutput.getEachNum());
        Assert.assertEquals(eachNum, receiverOutput.getEachNum());
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            Gf2kSspVoleSenderOutput eachSenderOutput = senderOutput.get(batchIndex);
            Gf2kSspVoleReceiverOutput eachReceiverOutput = receiverOutput.get(batchIndex);
            IntStream.range(0, eachNum).forEach(index -> {
                byte[] w = eachSenderOutput.getT(index);
                byte[] v = eachReceiverOutput.getQ(index);
                byte[] u = eachSenderOutput.getX(index);
                byte[] delta = eachReceiverOutput.getDelta();
                byte[] vPrime = BytesUtils.clone(delta);
                GF2K.muli(vPrime, u);
                GF2K.addi(vPrime, v);
                Assert.assertArrayEquals(w, vPrime);
                if (index == eachSenderOutput.getAlpha()) {
                    // u is non-zero
                    Assert.assertFalse(GF2K.isZero(u));
                } else {
                    // u is zero
                    Assert.assertTrue(GF2K.isZero(u));
                }
            });
        });
    }
}
