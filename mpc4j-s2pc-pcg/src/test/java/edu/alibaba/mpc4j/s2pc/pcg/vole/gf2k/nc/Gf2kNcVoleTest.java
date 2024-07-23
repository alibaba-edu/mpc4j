package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.VoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory.Gf2kNcVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21.Wykw21Gf2kNcVoleConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * GF2K-NC-VOLE tests.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
@RunWith(Parameterized.class)
public class Gf2kNcVoleTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kNcVoleTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 999;
    /**
     * default round
     */
    private static final int DEFAULT_ROUND = 1;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * large round
     */
    private static final int LARGE_ROUND = 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int subfieldL : new int[]{1, 2, 4, 8, 16, 32, 64, 128}) {
            // WYKW21
            configurations.add(new Object[]{
                Gf2kNcVoleType.WYKW21.name() + " (" + SecurityModel.MALICIOUS + ", subfieldL = " + subfieldL + ")",
                new Wykw21Gf2kNcVoleConfig.Builder(SecurityModel.MALICIOUS).build(), subfieldL,
            });
            configurations.add(new Object[]{
                Gf2kNcVoleType.WYKW21.name() + " (" + SecurityModel.SEMI_HONEST + ", subfieldL = " + subfieldL + ")",
                new Wykw21Gf2kNcVoleConfig.Builder(SecurityModel.SEMI_HONEST).build(), subfieldL,
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kNcVoleConfig config;
    /**
     * field
     */
    private final Sgf2k field;

    public Gf2kNcVoleTest(String name, Gf2kNcVoleConfig config, int subfieldL) {
        super(name);
        this.config = config;
        field = Sgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
    }

    @Test
    public void test1Round1Num() {
        testPto(1, 1, false);
    }

    @Test
    public void test2Round2Num() {
        testPto(2, 2, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_ROUND, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_ROUND, true);
    }

    @Test
    public void test12LogNum() {
        testPto(1 << 12, DEFAULT_ROUND, false);
    }

    @Test
    public void testLargeRound() {
        testPto(DEFAULT_NUM, LARGE_ROUND, false);
    }

    @Test
    public void testParallelLargeRound() {
        testPto(DEFAULT_NUM, LARGE_ROUND, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, DEFAULT_ROUND, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, DEFAULT_ROUND, true);
    }

    private void testPto(int num, int round, boolean parallel) {
        Gf2kNcVoleSender sender = Gf2kNcVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kNcVoleReceiver receiver = Gf2kNcVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            Gf2kNcVoleSenderThread senderThread = new Gf2kNcVoleSenderThread(sender, field, num, round);
            Gf2kNcVoleReceiverThread receiverThread = new Gf2kNcVoleReceiverThread(receiver, field, delta, num, round);
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
            Gf2kVoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kVoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            VoleTestUtils.assertOutput(field, num * round, senderOutput, receiverOutput);
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
