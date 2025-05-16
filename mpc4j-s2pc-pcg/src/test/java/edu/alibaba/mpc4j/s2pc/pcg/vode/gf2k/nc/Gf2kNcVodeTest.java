package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vode.VodeTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeFactory.Gf2kNcVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24.Aprr24Gf2kNcVodeConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * GF2K-NC-VODE tests.
 *
 * @author Weiran Liu
 * @date 2024/6/23
 */
@RunWith(Parameterized.class)
public class Gf2kNcVodeTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kNcVodeTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 999;
    /**
     * default round
     */
    private static final int DEFAULT_ROUND = 2;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 16) + 1;
    /**
     * large round
     */
    private static final int LARGE_ROUND = 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int subfieldL : new int[]{1, 2, 4, 8, 16, 32, 64, 128}) {
            // APRR24
            configurations.add(new Object[]{
                Gf2kNcVodeType.APRR24.name() + " (" + SecurityModel.SEMI_HONEST + ", subfieldL = " + subfieldL + ")",
                new Aprr24Gf2kNcVodeConfig.Builder(SecurityModel.SEMI_HONEST).build(), subfieldL,
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kNcVodeConfig config;
    /**
     * field
     */
    private final Dgf2k field;

    public Gf2kNcVodeTest(String name, Gf2kNcVodeConfig config, int subfieldL) {
        super(name);
        this.config = config;
        field = Dgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
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
        Gf2kNcVodeSender sender = Gf2kNcVodeFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kNcVodeReceiver receiver = Gf2kNcVodeFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = BlockUtils.randomBlock(SECURE_RANDOM);
            Gf2kNcVodeSenderThread senderThread = new Gf2kNcVodeSenderThread(sender, field, num, round);
            Gf2kNcVodeReceiverThread receiverThread = new Gf2kNcVodeReceiverThread(receiver, field, delta, num, round);
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
            Gf2kVodeSenderOutput senderOutput = senderThread.getSenderOutput();
            Gf2kVodeReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            VodeTestUtils.assertOutput(field, num * round, senderOutput, receiverOutput);
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
