package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory.Gf2kNcVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.direct.DirectGf2kNcVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21.Wykw21Gf2kNcVoleConfig;
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
    private static final int LARGE_ROUND = 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // WYKW21
        configurations.add(new Object[]{
            Gf2kNcVoleType.WYKW21.name() + " (" + SecurityModel.MALICIOUS + ")",
            new Wykw21Gf2kNcVoleConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[]{
            Gf2kNcVoleType.WYKW21.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new Wykw21Gf2kNcVoleConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // DIRECT
        configurations.add(new Object[]{
            Gf2kNcVoleType.DIRECT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectGf2kNcVoleConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[]{
            Gf2kNcVoleType.DIRECT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectGf2kNcVoleConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }
    /**
     * config
     */
    private final Gf2kNcVoleConfig config;

    public Gf2kNcVoleTest(String name, Gf2kNcVoleConfig config) {
        super(name);
        this.config = config;
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
    public void testDefaultRoundDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_ROUND, false);
    }

    @Test
    public void testParallelDefaultRoundDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_ROUND, true);
    }

    @Test
    public void test12LogNum() {
        testPto(1 << 12, DEFAULT_ROUND, false);
    }

    @Test
    public void testLargeRoundDefaultNum() {
        testPto(DEFAULT_NUM, LARGE_ROUND, false);
    }

    @Test
    public void testParallelLargeRoundDefaultNum() {
        testPto(DEFAULT_NUM, LARGE_ROUND, true);
    }

    @Test
    public void testDefaultRoundLargeNum() {
        testPto(LARGE_NUM, DEFAULT_ROUND, false);
    }

    @Test
    public void testParallelDefaultRoundLargeNum() {
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
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            Gf2kNcVoleSenderThread senderThread = new Gf2kNcVoleSenderThread(sender, num, round);
            Gf2kNcVoleReceiverThread receiverThread = new Gf2kNcVoleReceiverThread(receiver, delta, num, round);
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
            Gf2kVoleTestUtils.assertOutput(num * round, senderOutput, receiverOutput);
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
    public void testResetDelta() {
        Gf2kNcVoleSender sender = Gf2kNcVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kNcVoleReceiver receiver = Gf2kNcVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        int round = DEFAULT_ROUND;
        int num = DEFAULT_NUM;
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            // first round
            Gf2kNcVoleSenderThread senderThread = new Gf2kNcVoleSenderThread(sender, num, round);
            Gf2kNcVoleReceiverThread receiverThread = new Gf2kNcVoleReceiverThread(receiver, delta, num, round);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kVoleSenderOutput firstSenderOutput = senderThread.getSenderOutput();
            Gf2kVoleReceiverOutput firstReceiverOutput = receiverThread.getReceiverOutput();
            Gf2kVoleTestUtils.assertOutput(num * round, firstSenderOutput, firstReceiverOutput);
            printAndResetRpc(firstTime);
            // second time, reset delta
            SECURE_RANDOM.nextBytes(delta);
            senderThread = new Gf2kNcVoleSenderThread(sender, num, round);
            receiverThread = new Gf2kNcVoleReceiverThread(receiver, delta, num, round);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            Gf2kVoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Gf2kVoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            Gf2kVoleTestUtils.assertOutput(num * round, secondSenderOutput, secondReceiverOutput);
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
}
