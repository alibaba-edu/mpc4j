package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory.PreCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * pre-compute 1-out-of-n (with n = 2^l) test.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
@RunWith(Parameterized.class)
public class PreCotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreCotTest.class);
    /**
     * the default num
     */
    private static final int DEFAULT_NUM = 1000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Bea95
        configurations.add(new Object[] {PreCotType.Bea95.name(), new Bea95PreCotConfig.Builder().build(),});

        return configurations;
    }

    /**
     * the config
     */
    private final PreCotConfig config;

    public PreCotTest(String name, PreCotConfig config) {
        super(name);
        this.config = config;
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
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        PreCotSender sender = PreCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PreCotReceiver receiver = PreCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            // pre-compute sender / receiver output
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            CotSenderOutput preSenderOutput = CotTestUtils.genSenderOutput(num, delta, SECURE_RANDOM);
            CotReceiverOutput preReceiverOutput = CotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
            // receiver actual choices
            boolean[] choices = new boolean[num];
            IntStream.range(0, num).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            PreCotSenderThread senderThread = new PreCotSenderThread(sender, preSenderOutput);
            PreCotReceiverThread receiverThread = new PreCotReceiverThread(receiver, preReceiverOutput, choices);
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
            CotSenderOutput senderOutput = senderThread.getSenderOutput();
            CotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
