package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.VodeTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory.Gf2kCoreVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.aprr24.Aprr24Gf2kCoreVodeConfig;
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
 * GF2K-core-VODE test.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
@RunWith(Parameterized.class)
public class Gf2kCoreVodeTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kCoreVodeTest.class);
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
            // APRR24
            configurations.add(new Object[]{
                Gf2kCoreVodeType.APRR24.name() + " (subfieldL = " + subfieldL + ")",
                new Aprr24Gf2kCoreVodeConfig.Builder().build(), subfieldL}
            );
        }

        return configurations;
    }

    /**
     * the protocol config
     */
    private final Gf2kCoreVodeConfig config;
    /**
     * field
     */
    private final Dgf2k field;
    /**
     * subfield
     */
    private final Gf2e subfield;

    public Gf2kCoreVodeTest(String name, Gf2kCoreVodeConfig config, int subfieldL) {
        super(name);
        this.config = config;
        field = Dgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
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
        Gf2kCoreVodeSender sender = Gf2kCoreVodeFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Gf2kCoreVodeReceiver receiver = Gf2kCoreVodeFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
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
            Gf2kCoreVodeSenderThread senderThread = new Gf2kCoreVodeSenderThread(sender, field, x);
            Gf2kCoreVodeReceiverThread receiverThread = new Gf2kCoreVodeReceiverThread(receiver, field, delta, num);
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
}
