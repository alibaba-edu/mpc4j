package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.VoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.Gf2kMspVoleFactory.Gf2kMspVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.bcg19.Bcg19RegGf2kMspVoleConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

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
     * default sparse num
     */
    private static final int DEFAULT_T = (1 << 4) - 1;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = (1 << 10) + 1;
    /**
     * large spare num
     */
    private static final int LARGE_T = (1 << 10) - 1;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 16) + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int subfieldL : new int[]{1, 2, 4, 8, 16, 32, 64, 128}) {
            // BCG19_REG
            configurations.add(new Object[]{
                Gf2kMspVoleType.BCG19_REG + " (" + SecurityModel.MALICIOUS + ", subfieldL = " + subfieldL + ")",
                new Bcg19RegGf2kMspVoleConfig.Builder(SecurityModel.MALICIOUS).build(), subfieldL,
            });
            configurations.add(new Object[]{
                Gf2kMspVoleType.BCG19_REG + " (" + SecurityModel.SEMI_HONEST + ", subfieldL = " + subfieldL + ")",
                new Bcg19RegGf2kMspVoleConfig.Builder(SecurityModel.SEMI_HONEST).build(), subfieldL,
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final Gf2kMspVoleConfig config;
    /**
     * field
     */
    private final Sgf2k field;

    public Gf2kMspVoleTest(String name, Gf2kMspVoleConfig config, int subfieldL) {
        super(name);
        this.config = config;
        field = Sgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
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
            byte[] delta = BlockUtils.randomBlock(SECURE_RANDOM);
            Gf2kMspVoleSenderThread senderThread = new Gf2kMspVoleSenderThread(sender, field, t, num);
            Gf2kMspVoleReceiverThread receiverThread = new Gf2kMspVoleReceiverThread(receiver, field, delta, t, num);
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
}
