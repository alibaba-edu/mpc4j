package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95.Bea95PreLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotFactory.PreLnotType;
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
 * pre-compute 1-out-of-n (with n = 2^l) OT test.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
@RunWith(Parameterized.class)
public class PreLnotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreLnotTest.class);
    /**
     * the default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * default l
     */
    private static final int DEFAULT_L = 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Bea95
        configurations.add(new Object[] {PreLnotType.Bea95.name(), new Bea95PreLnotConfig.Builder().build(),});

        return configurations;
    }

    /**
     * the config
     */
    private final PreLnotConfig config;

    public PreLnotTest(String name, PreLnotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(1, DEFAULT_L, false);
    }

    @Test
    public void test2Num() {
        testPto(2, DEFAULT_L, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_L, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_L, true);
    }

    @Test
    public void testSmallL() {
        testPto(DEFAULT_NUM, 1, false);
    }

    @Test
    public void testLargeL() {
        testPto(DEFAULT_NUM, 10, false);
    }

    @Test
    public void testLargeNum() {
        testPto(1 << 18, DEFAULT_L, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(1 << 18, DEFAULT_L, true);
    }

    private void testPto(int num, int l, boolean parallel) {
        int n = (1 << l);
        PreLnotSender sender = PreLnotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PreLnotReceiver receiver = PreLnotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
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
            LnotSenderOutput preSenderOutput = LnotTestUtils.genSenderOutput(l, num, SECURE_RANDOM);
            LnotReceiverOutput preReceiverOutput = LnotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
            // receiver actual choices
            int[] choiceArray = IntStream.range(0, num)
                .map(index -> SECURE_RANDOM.nextInt(n))
                .toArray();
            PreLnotSenderThread senderThread = new PreLnotSenderThread(sender, preSenderOutput);
            PreLnotReceiverThread receiverThread = new PreLnotReceiverThread(receiver, preReceiverOutput, choiceArray);
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
            LnotSenderOutput senderOutput = senderThread.getSenderOutput();
            LnotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            LnotTestUtils.assertOutput(l, num, senderOutput, receiverOutput);
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
