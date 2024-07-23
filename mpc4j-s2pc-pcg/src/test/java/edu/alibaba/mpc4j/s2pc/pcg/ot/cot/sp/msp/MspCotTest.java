package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotFactory.MspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.ywl20.Ywl20UniMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.bcg19.Bcg19RegMspCotConfig.Builder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MSP-COT tests.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
@RunWith(Parameterized.class)
public class MspCotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(MspCotTest.class);
    /**
     * default sparse num
     */
    private static final int DEFAULT_T = 1 << 4;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1 << 10;
    /**
     * large spare num
     */
    private static final int LARGE_T = 1 << 10;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // BCG19_REG
        configurations.add(new Object[] {
            MspCotType.BCG19_REG.name() + " (" + SecurityModel.MALICIOUS.name() + ")",
            new Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[] {
            MspCotType.BCG19_REG.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20_UNI
        configurations.add(new Object[] {
            MspCotType.YWL20_UNI.name() + " (" + SecurityModel.MALICIOUS.name() + ")",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[] {
            MspCotType.YWL20_UNI.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final MspCotConfig config;

    public MspCotTest(String name, MspCotConfig config) {
        super(name);
        this.config = config;
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
        MspCotSender sender = MspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            MspCotSenderThread senderThread = new MspCotSenderThread(sender, delta, t, num);
            MspCotReceiverThread receiverThread = new MspCotReceiverThread(receiver, t, num);
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
            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
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
    public void testPrecomputeLargeNumLargeT() {
        MspCotSender sender = MspCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        MspCotReceiver receiver = MspCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            int num = LARGE_NUM;
            int t = LARGE_T;
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            CotSenderOutput preSenderOutput = CotSenderOutput.createRandom(
                MspCotFactory.getPrecomputeNum(config, t, num), delta, SECURE_RANDOM
            );
            CotReceiverOutput preReceiverOutput = CotReceiverOutput.createRandom(preSenderOutput, SECURE_RANDOM);
            MspCotSenderThread senderThread = new MspCotSenderThread(sender, delta, t, num, preSenderOutput);
            MspCotReceiverThread receiverThread = new MspCotReceiverThread(receiver, t, num, preReceiverOutput);
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
            MspCotSenderOutput senderOutput = senderThread.getSenderOutput();
            MspCotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int num, MspCotSenderOutput senderOutput, MspCotReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        Set<Integer> alphaSet = Arrays.stream(receiverOutput.getAlphaArray()).boxed().collect(Collectors.toSet());
        IntStream.range(0, num).forEach(index -> {
            if (alphaSet.contains(index)) {
                Assert.assertArrayEquals(senderOutput.getR1(index), receiverOutput.getRb(index));
            } else {
                Assert.assertArrayEquals(senderOutput.getR0(index), receiverOutput.getRb(index));
            }
        });
    }
}
