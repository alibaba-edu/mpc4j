package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.VoleTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory.Gf2kCoreVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16.Kos16Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21.Wykw21Gf2kCoreVoleConfig;
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
 * GF2K-core-VOLE tests.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
@RunWith(Parameterized.class)
public class Gf2kCoreVoleTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kCoreVoleTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 127;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 10) - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int subfieldL : new int[]{1, 2, 4, 8, 16, 32, 64, 128}) {
            // WYKW21
            configurations.add(new Object[]{
                Gf2kCoreVoleType.WYKW21.name() + " (subfieldL = " + subfieldL + ")",
                new Wykw21Gf2kCoreVoleConfig.Builder().build(), subfieldL}
            );
            // KOS16
            configurations.add(new Object[]{
                Gf2kCoreVoleType.KOS16.name() + " (subfieldL = " + subfieldL + ")",
                new Kos16Gf2kCoreVoleConfig.Builder().build(), subfieldL}
            );
        }

        return configurations;
    }

    /**
     * the protocol config
     */
    private final Gf2kCoreVoleConfig config;
    /**
     * field
     */
    private final Sgf2k field;
    /**
     * subfield
     */
    private final Gf2e subfield;

    public Gf2kCoreVoleTest(String name, Gf2kCoreVoleConfig config, int subfieldL) {
        super(name);
        this.config = config;
        field = Sgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
        subfield = field.getSubfield();
    }

    @Test
    public void test1Num() {
        testPto(1, false);
    }

    @Test
    public void test2Num() {
        testPto(2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        Gf2kCoreVoleSender sender = Gf2kCoreVoleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kCoreVoleReceiver receiver = Gf2kCoreVoleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = field.createRangeRandom(SECURE_RANDOM);
            byte[][] x = IntStream.range(0, num)
                .mapToObj(index -> subfield.createRandom(SECURE_RANDOM))
                .toArray(byte[][]::new);
            Gf2kCoreVoleSenderThread senderThread = new Gf2kCoreVoleSenderThread(sender, field, x);
            Gf2kCoreVoleReceiverThread receiverThread = new Gf2kCoreVoleReceiverThread(receiver, field, delta, num);
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
