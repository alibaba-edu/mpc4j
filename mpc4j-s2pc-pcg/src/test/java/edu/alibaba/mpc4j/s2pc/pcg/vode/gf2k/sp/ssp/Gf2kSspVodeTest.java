package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.VodeTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeFactory.Gf2kSspVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.aprr24.Aprr24Gf2kSspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.gyw23.Gyw23Gf2kSspVodeConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Single single-point GF2K-VODE tests.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
@RunWith(Parameterized.class)
public class Gf2kSspVodeTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kSspVodeTest.class);
    /**
     * default num, the num is not even, and not in format 2^k
     */
    private static final int DEFAULT_NUM = 9;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 16) - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int subfieldL : new int[]{1, 2, 4, 8, 16, 32, 64, 128}) {

            // GYW23
            configurations.add(new Object[]{
                Gf2kSspVodeType.GYW23.name() + "(subfieldL = " + subfieldL + ")",
                new Gyw23Gf2kSspVodeConfig.Builder().build(), subfieldL,
            });
            // APRR24
            configurations.add(new Object[]{
                Gf2kSspVodeType.APRR24.name() + "(subfieldL = " + subfieldL + ")",
                new Aprr24Gf2kSspVodeConfig.Builder().build(), subfieldL,
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kSspVodeConfig config;
    /**
     * field
     */
    private final Dgf2k field;

    public Gf2kSspVodeTest(String name, Gf2kSspVodeConfig config, int subfieldL) {
        super(name);
        this.config = config;
        field = Dgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
    }

    @Test
    public void testFirstAlpha() {
        //noinspection UnnecessaryLocalVariable
        int num = DEFAULT_NUM;
        int alpha = 0;
        testPto(alpha, num, false);
    }

    @Test
    public void testLastAlpha() {
        int num = DEFAULT_NUM;
        int alpha = num - 1;
        testPto(alpha, num, false);
    }

    @Test
    public void test1Num() {
        int num = 1;
        int alpha = 0;
        testPto(alpha, num, false);
    }

    @Test
    public void test2Num() {
        int num = 2;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void test4Num() {
        int num = 4;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testDefault() {
        int num = DEFAULT_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testParallelDefault() {
        int num = DEFAULT_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, true);
    }

    @Test
    public void testLargeNum() {
        int num = LARGE_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testParallelLargeNum() {
        int num = LARGE_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, true);
    }

    private void testPto(int alpha, int num, boolean parallel) {
        Gf2kSspVodeSender sender = Gf2kSspVodeFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kSspVodeReceiver receiver = Gf2kSspVodeFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = field.createRangeRandom(SECURE_RANDOM);
            Gf2kSspVodeSenderThread senderThread = new Gf2kSspVodeSenderThread(sender, field, alpha, num);
            Gf2kSspVodeReceiverThread receiverThread = new Gf2kSspVodeReceiverThread(receiver, field, delta, num);
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
            Gf2kSspVodeSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kSspVodeReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            VodeTestUtils.assertOutput(field, num, senderOutput, receiverOutput);
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
        Gf2kSspVodeSender sender = Gf2kSspVodeFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kSspVodeReceiver receiver = Gf2kSspVodeFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = field.createRangeRandom(SECURE_RANDOM);
            int alpha = SECURE_RANDOM.nextInt(num);
            Gf2kVodeReceiverOutput preReceiverOutput = Gf2kVodeReceiverOutput.createRandom(
                field, Gf2kSspVodeFactory.getPrecomputeNum(config, field.getSubfieldL(), num), delta, SECURE_RANDOM
            );
            Gf2kVodeSenderOutput preSenderOutput = Gf2kVodeSenderOutput.createRandom(preReceiverOutput, SECURE_RANDOM);
            Gf2kSspVodeSenderThread senderThread = new Gf2kSspVodeSenderThread(sender, field, alpha, num, preSenderOutput);
            Gf2kSspVodeReceiverThread receiverThread = new Gf2kSspVodeReceiverThread(receiver, field, delta, num, preReceiverOutput);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kSspVodeSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kSspVodeReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            VodeTestUtils.assertOutput(field, num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
