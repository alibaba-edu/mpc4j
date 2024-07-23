package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.VodeTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory.Gf2kBspVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24.Aprr24Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.gyw23.Gyw23Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeSenderOutput;
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
 * GF2K-BSP-VODE tests.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
@RunWith(Parameterized.class)
public class Gf2kBspVodeTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kBspVodeTest.class);
    /**
     * default num, the num is not even, and not in format 2^k
     */
    private static final int DEFAULT_EACH_NUM = 9;
    /**
     * large num
     */
    private static final int LARGE_EACH_NUM = (1 << 10) - 1;
    /**
     * default batch num, we select an odd number and not with the format 2^k
     */
    private static final int DEFAULT_BATCH_NUM = 9;
    /**
     * large batch num
     */
    private static final int LARGE_BATCH_NUM = (1 << 10) - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int subfieldL : new int[]{1, 2, 4, 8, 16, 32, 64, 128}) {
            // GYW23
            configurations.add(new Object[]{
                Gf2kBspVodeType.GYW23.name() + "(subfieldL = " + subfieldL + ")",
                new Gyw23Gf2kBspVodeConfig.Builder().build(), subfieldL,
            });
            // APRR24
            configurations.add(new Object[]{
                Gf2kBspVodeType.APRR24.name() + "(subfieldL = " + subfieldL + ")",
                new Aprr24Gf2kBspVodeConfig.Builder().build(), subfieldL,
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kBspVodeConfig config;
    /**
     * field
     */
    private final Dgf2k field;

    public Gf2kBspVodeTest(String name, Gf2kBspVodeConfig config, int subfieldL) {
        super(name);
        this.config = config;
        field = Dgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
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
    public void test4EachNum() {
        int eachNum = 4;
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
        Gf2kBspVodeSender sender = Gf2kBspVodeFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kBspVodeReceiver receiver = Gf2kBspVodeFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = field.createRangeRandom(SECURE_RANDOM);
            int batchNum = alphaArray.length;
            Gf2kBspVodeSenderThread senderThread = new Gf2kBspVodeSenderThread(sender, field, alphaArray, num);
            Gf2kBspVodeReceiverThread receiverThread = new Gf2kBspVodeReceiverThread(receiver, field, delta, batchNum, num);
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
            Gf2kBspVodeSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kBspVodeReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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
        Gf2kBspVodeSender sender = Gf2kBspVodeFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kBspVodeReceiver receiver = Gf2kBspVodeFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = DEFAULT_BATCH_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = field.createRangeRandom(SECURE_RANDOM);
            int[] alphaArray = IntStream.range(0, batchNum)
                .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
                .toArray();
            Gf2kVodeReceiverOutput preReceiverOutput = Gf2kVodeReceiverOutput.createRandom(
                field, Gf2kBspVodeFactory.getPrecomputeNum(config, field.getSubfieldL(), batchNum, eachNum), delta, SECURE_RANDOM
            );
            Gf2kVodeSenderOutput preSenderOutput = Gf2kVodeSenderOutput.createRandom(preReceiverOutput, SECURE_RANDOM);
            Gf2kBspVodeSenderThread senderThread = new Gf2kBspVodeSenderThread(
                sender, field, alphaArray, eachNum, preSenderOutput
            );
            Gf2kBspVodeReceiverThread receiverThread = new Gf2kBspVodeReceiverThread(
                receiver, field, delta, batchNum, eachNum, preReceiverOutput
            );
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kBspVodeSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kBspVodeReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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

    private void assertOutput(int batchNum, int eachNum,
                              Gf2kBspVodeSenderOutput senderOutput, Gf2kBspVodeReceiverOutput receiverOutput) {
        Assert.assertEquals(batchNum, senderOutput.getBatchNum());
        Assert.assertEquals(batchNum, receiverOutput.getBatchNum());
        Assert.assertEquals(eachNum, senderOutput.getEachNum());
        Assert.assertEquals(eachNum, receiverOutput.getEachNum());
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            Gf2kSspVodeSenderOutput eachSenderOutput = senderOutput.get(batchIndex);
            Gf2kSspVodeReceiverOutput eachReceiverOutput = receiverOutput.get(batchIndex);
            VodeTestUtils.assertOutput(field, eachNum, eachSenderOutput, eachReceiverOutput);
        });
    }
}
