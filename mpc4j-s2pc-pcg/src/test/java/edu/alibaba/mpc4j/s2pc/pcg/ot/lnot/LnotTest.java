package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory.LnotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.direct.DirectLnotConfig;
import org.junit.Assert;
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
 * 1-out-of-n (with n = 2^l) OT test.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
@RunWith(Parameterized.class)
public class LnotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(LnotTest.class);
    /**
     * default l
     */
    private static final int DEFAULT_L = 5;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * small l
     */
    private static final int SMALL_L = 1;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CACHE
        configurations.add(new Object[]{
            LnotType.CACHE.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectLnotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[]{
            LnotType.CACHE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectLnotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // DIRECT
        configurations.add(new Object[]{
            LnotType.DIRECT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectLnotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[]{
            LnotType.DIRECT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectLnotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final LnotConfig config;

    public LnotTest(String name, LnotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Round1Num() {
        testPto(DEFAULT_L, 1, false);
    }

    @Test
    public void test2Round2Num() {
        testPto(DEFAULT_L, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_L, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_L, DEFAULT_NUM, true);
    }

    @Test
    public void testSmallL() {
        testPto(SMALL_L, DEFAULT_NUM, false);
    }

    @Test
    public void testLog12Num() {
        testPto(DEFAULT_L, 1 << 12, false);
    }

    @Test
    public void testLog16Num() {
        testPto(DEFAULT_L, 1 << 16, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, true);
    }

    private void testPto(int l, int num, boolean parallel) {
        LnotSender sender = LnotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        LnotReceiver receiver = LnotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate the input
        int n = (1 << l);
        int[] choiceArray = IntStream.range(0, num)
            .map(index -> SECURE_RANDOM.nextInt(n))
            .toArray();
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            LnotSenderThread senderThread = new LnotSenderThread(sender, l, num);
            LnotReceiverThread receiverThread = new LnotReceiverThread(receiver, l, choiceArray);
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
            int[] actualChoiceArray = IntStream.range(0, num)
                .map(receiverOutput::getChoice)
                .toArray();
            Assert.assertArrayEquals(choiceArray, actualChoiceArray);
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
    public void testLessUpdate() {
        int l = DEFAULT_L;
        int num = DEFAULT_NUM;
        LnotSender sender = LnotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        LnotReceiver receiver = LnotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate the input
        int n = (1 << l);
        int[] choiceArray = IntStream.range(0, num)
            .map(index -> SECURE_RANDOM.nextInt(n))
            .toArray();
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            LnotSenderThread senderThread = new LnotSenderThread(sender, l, num, num / 2 - 1);
            LnotReceiverThread receiverThread = new LnotReceiverThread(receiver, l, choiceArray, num / 2 - 1);
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
            int[] actualChoiceArray = IntStream.range(0, num)
                .map(receiverOutput::getChoice)
                .toArray();
            Assert.assertArrayEquals(choiceArray, actualChoiceArray);
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
