package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.silent.SilentCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * COT test.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
@RunWith(Parameterized.class)
public class CotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(CotTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // DIRECT
        configurations.add(new Object[] {
            CotFactory.CotType.DIRECT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        configurations.add(new Object[] {
            CotFactory.CotType.DIRECT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // CACHE
        configurations.add(new Object[] {
            CotFactory.CotType.SILENT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new SilentCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        configurations.add(new Object[] {
            CotFactory.CotType.SILENT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new SilentCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });

        return configurations;
    }
    /**
     * 协议类型
     */
    private final CotConfig config;

    public CotTest(String name, CotConfig config) {
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

    @Test
    public void testLargeNum() {
        testPto(1 << 20, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(1 << 20, true);
    }

    private void testPto(int num, boolean parallel) {
        CotSender sender = CotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            boolean[] choices = BinaryUtils.randomBinary(num, SECURE_RANDOM);
            CotSenderThread senderThread = new CotSenderThread(sender, delta, num);
            CotReceiverThread receiverThread = new CotReceiverThread(receiver, choices);
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
            OtTestUtils.assertOutput(num, senderOutput, receiverOutput);
            Assert.assertArrayEquals(delta, senderOutput.getDelta());
            Assert.assertArrayEquals(choices, receiverOutput.getChoices());
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
    public void testMultipleRound() {
        int num = DEFAULT_NUM;
        CotSender sender = CotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        CotReceiver receiver = CotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (multiple round) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            boolean[] choices = BinaryUtils.randomBinary(DEFAULT_NUM, SECURE_RANDOM);
            CotSenderThread senderThread = new CotSenderThread(sender, delta, num, num / 2 - 1);
            CotReceiverThread receiverThread = new CotReceiverThread(receiver, choices, num / 2 - 1);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            CotSenderOutput senderOutput = senderThread.getSenderOutput();
            CotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            OtTestUtils.assertOutput(num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (multiple round) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
