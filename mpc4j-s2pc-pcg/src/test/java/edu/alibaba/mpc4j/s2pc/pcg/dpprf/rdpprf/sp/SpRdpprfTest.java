package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.ywl20.Ywl20SpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory.SpRdpprfType;
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
 * single-point RDPPRF tests.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
@RunWith(Parameterized.class)
public class SpRdpprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpRdpprfTest.class);
    /**
     * default n, which is not even, and not in format 2^k
     */
    private static final int DEFAULT_NUM = 15;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // YWL20 (semi-honest)
        configurations.add(new Object[]{
            SpRdpprfType.YWL20.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new Ywl20SpRdpprfConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20 (malicious)
        configurations.add(new Object[]{
            SpRdpprfType.YWL20.name() + " (" + SecurityModel.MALICIOUS + ")",
            new Ywl20SpRdpprfConfig.Builder(SecurityModel.MALICIOUS).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final SpRdpprfConfig config;

    public SpRdpprfTest(String name, SpRdpprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testFirstAlpha() {
        //noinspection UnnecessaryLocalVariable
        int num = DEFAULT_NUM;
        int alpha = 0;
        testPto(alpha, num, false);
    }

    @Test
    public void testLastAlpha() {
        int num = DEFAULT_NUM;
        int alpha = num - 1;
        testPto(alpha, num, false);
    }

    @Test
    public void test1AlphaBound() {
        int num = 1;
        int alpha = 0;
        testPto(alpha, num, false);
    }

    @Test
    public void test2AlphaBound() {
        int num = 2;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testDefault() {
        int num = DEFAULT_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testParallelDefault() {
        int num = DEFAULT_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, true);
    }

    @Test
    public void testLargeAlphaBound() {
        int num = LARGE_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testParallelLargeAlphaBound() {
        int num = LARGE_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, true);
    }

    private void testPto(int alpha, int num, boolean parallel) {
        SpRdpprfSender sender = SpRdpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SpRdpprfReceiver receiver = SpRdpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            SpRdpprfSenderThread senderThread = new SpRdpprfSenderThread(sender, num);
            SpRdpprfReceiverThread receiverThread = new SpRdpprfReceiverThread(receiver, alpha, num);
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
            SpRdpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            SpRdpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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
    public void testPrecompute() {
        SpRdpprfSender sender = SpRdpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SpRdpprfReceiver receiver = SpRdpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int alphaBound = DEFAULT_NUM;
        int alpha = SECURE_RANDOM.nextInt(alphaBound);
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
        // pre-compute COT
        CotSenderOutput preSenderOutput = CotSenderOutput.createRandom(
            SpRdpprfFactory.getPrecomputeNum(config, alphaBound), delta, SECURE_RANDOM
        );
        CotReceiverOutput preReceiverOutput = CotReceiverOutput.createRandom(preSenderOutput, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            SpRdpprfSenderThread senderThread = new SpRdpprfSenderThread(sender, alphaBound, preSenderOutput);
            SpRdpprfReceiverThread receiverThread = new SpRdpprfReceiverThread(
                receiver, alpha, alphaBound, preReceiverOutput
            );
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
            SpRdpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            SpRdpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(alphaBound, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int num, SpRdpprfSenderOutput senderOutput, SpRdpprfReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        byte[][] prfKey = senderOutput.getV0Array();
        byte[][] pprfKey = receiverOutput.getV1Array();
        IntStream.range(0, num).forEach(index -> {
            if (index == receiverOutput.getAlpha()) {
                Assert.assertNull(pprfKey[index]);
            } else {
                Assert.assertArrayEquals(prfKey[index], pprfKey[index]);
            }
        });
    }
}
