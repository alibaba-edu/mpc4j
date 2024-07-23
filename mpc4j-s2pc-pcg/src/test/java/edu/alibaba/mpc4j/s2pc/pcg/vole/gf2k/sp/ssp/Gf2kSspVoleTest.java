package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.VoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleFactory.Gf2kSspVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.gyw23.Gyw23Gf2kSspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.wykw21.Wykw21MaGf2kSspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.wykw21.Wykw21ShGf2kSspVoleConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Single single-point GF2K-VOLE tests.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
@RunWith(Parameterized.class)
public class Gf2kSspVoleTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kSspVoleTest.class);
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
                Gf2kSspVoleType.GYW23.name() + "(subfieldL = " + subfieldL + ")",
                new Gyw23Gf2kSspVoleConfig.Builder().build(), subfieldL,
            });
            // WYKW21_MALICIOUS
            configurations.add(new Object[]{
                Gf2kSspVoleType.WYKW21_MALICIOUS.name() + "(subfieldL = " + subfieldL + ")",
                new Wykw21MaGf2kSspVoleConfig.Builder().build(), subfieldL,
            });
            // WYKW21_SEMI_HONEST
            configurations.add(new Object[]{
                Gf2kSspVoleType.WYKW21_SEMI_HONEST.name() + "(subfieldL = " + subfieldL + ")",
                new Wykw21ShGf2kSspVoleConfig.Builder().build(), subfieldL,
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kSspVoleConfig config;
    /**
     * field
     */
    private final Sgf2k field;

    public Gf2kSspVoleTest(String name, Gf2kSspVoleConfig config, int subfieldL) {
        super(name);
        this.config = config;
        field = Sgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
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
        Gf2kSspVoleSender sender = Gf2kSspVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kSspVoleReceiver receiver = Gf2kSspVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = field.createRangeRandom(SECURE_RANDOM);
            Gf2kSspVoleSenderThread senderThread = new Gf2kSspVoleSenderThread(sender, field, alpha, num);
            Gf2kSspVoleReceiverThread receiverThread = new Gf2kSspVoleReceiverThread(receiver, field, delta, num);
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
            Gf2kSspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kSspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            VoleTestUtils.assertOutput(field, num, senderOutput, receiverOutput);
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
        Gf2kSspVoleSender sender = Gf2kSspVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kSspVoleReceiver receiver = Gf2kSspVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = field.createRangeRandom(SECURE_RANDOM);
            int alpha = SECURE_RANDOM.nextInt(num);
            Gf2kVoleReceiverOutput preReceiverOutput = Gf2kVoleReceiverOutput.createRandom(
                field, Gf2kSspVoleFactory.getPrecomputeNum(config, field.getSubfieldL(), num), delta, SECURE_RANDOM
            );
            Gf2kVoleSenderOutput preSenderOutput = Gf2kVoleSenderOutput.createRandom(preReceiverOutput, SECURE_RANDOM);
            Gf2kSspVoleSenderThread senderThread = new Gf2kSspVoleSenderThread(sender, field, alpha, num, preSenderOutput);
            Gf2kSspVoleReceiverThread receiverThread = new Gf2kSspVoleReceiverThread(receiver, field, delta, num, preReceiverOutput);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kSspVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kSspVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            VoleTestUtils.assertOutput(field, num, senderOutput, receiverOutput);
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
