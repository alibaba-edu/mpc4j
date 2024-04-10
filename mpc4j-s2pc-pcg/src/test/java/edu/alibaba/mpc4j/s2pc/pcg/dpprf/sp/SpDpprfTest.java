package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.ywl20.Ywl20SpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory.SpDpprfType;
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
 * single-point DPPRF tests.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
@RunWith(Parameterized.class)
public class SpDpprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpDpprfTest.class);
    /**
     * default α bound, the bound is not even, and not in format 2^k
     */
    private static final int DEFAULT_ALPHA_BOUND = 15;
    /**
     * large α bound
     */
    private static final int LARGE_ALPHA_BOUND = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // YWL20 (semi-honest)
        configurations.add(new Object[]{
            SpDpprfType.YWL20.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new Ywl20SpDpprfConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20 (malicious)
        configurations.add(new Object[]{
            SpDpprfType.YWL20.name() + " (" + SecurityModel.MALICIOUS + ")",
            new Ywl20SpDpprfConfig.Builder(SecurityModel.MALICIOUS).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final SpDpprfConfig config;

    public SpDpprfTest(String name, SpDpprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testFirstAlpha() {
        //noinspection UnnecessaryLocalVariable
        int alphaBound = DEFAULT_ALPHA_BOUND;
        int alpha = 0;
        testPto(alpha, alphaBound, false);
    }

    @Test
    public void testLastAlpha() {
        int alphaBound = DEFAULT_ALPHA_BOUND;
        int alpha = alphaBound - 1;
        testPto(alpha, alphaBound, false);
    }

    @Test
    public void test1AlphaBound() {
        int alphaBound = 1;
        int alpha = 0;
        testPto(alpha, alphaBound, false);
    }

    @Test
    public void test2AlphaBound() {
        int alphaBound = 2;
        int alpha = SECURE_RANDOM.nextInt(alphaBound);
        testPto(alpha, alphaBound, false);
    }

    @Test
    public void testDefault() {
        int alphaBound = DEFAULT_ALPHA_BOUND;
        int alpha = SECURE_RANDOM.nextInt(alphaBound);
        testPto(alpha, alphaBound, false);
    }

    @Test
    public void testParallelDefault() {
        int alphaBound = DEFAULT_ALPHA_BOUND;
        int alpha = SECURE_RANDOM.nextInt(alphaBound);
        testPto(alpha, alphaBound, true);
    }

    @Test
    public void testLargeAlphaBound() {
        int alphaBound = LARGE_ALPHA_BOUND;
        int alpha = SECURE_RANDOM.nextInt(alphaBound);
        testPto(alpha, alphaBound, false);
    }

    @Test
    public void testParallelLargeAlphaBound() {
        int alphaBound = LARGE_ALPHA_BOUND;
        int alpha = SECURE_RANDOM.nextInt(alphaBound);
        testPto(alpha, alphaBound, true);
    }

    private void testPto(int alpha, int alphaBound, boolean parallel) {
        SpDpprfSender sender = SpDpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SpDpprfReceiver receiver = SpDpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            SpDpprfSenderThread senderThread = new SpDpprfSenderThread(sender, alphaBound);
            SpDpprfReceiverThread receiverThread = new SpDpprfReceiverThread(receiver, alpha, alphaBound);
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
            SpDpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            SpDpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(alphaBound, senderOutput, receiverOutput);
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
        SpDpprfSender sender = SpDpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SpDpprfReceiver receiver = SpDpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int alphaBound = DEFAULT_ALPHA_BOUND;
        int alpha = SECURE_RANDOM.nextInt(alphaBound);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        // pre-compute COT
        CotSenderOutput preSenderOutput = CotTestUtils.genSenderOutput(
            SpDpprfFactory.getPrecomputeNum(config, alphaBound), delta, SECURE_RANDOM
        );
        CotReceiverOutput preReceiverOutput = CotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            SpDpprfSenderThread senderThread = new SpDpprfSenderThread(sender, alphaBound, preSenderOutput);
            SpDpprfReceiverThread receiverThread = new SpDpprfReceiverThread(
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
            SpDpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            SpDpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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

    private void assertOutput(int alphaBound, SpDpprfSenderOutput senderOutput, SpDpprfReceiverOutput receiverOutput) {
        Assert.assertEquals(1, senderOutput.getNum());
        Assert.assertEquals(1, receiverOutput.getNum());
        Assert.assertEquals(alphaBound, senderOutput.getAlphaBound());
        Assert.assertEquals(alphaBound, receiverOutput.getAlphaBound());
        byte[][] prfKey = senderOutput.getPrfKeys();
        byte[][] pprfKey = receiverOutput.getPprfKeys();
        IntStream.range(0, alphaBound).forEach(index -> {
            if (index == receiverOutput.getAlpha()) {
                Assert.assertNull(pprfKey[index]);
            } else {
                Assert.assertArrayEquals(prfKey[index], pprfKey[index]);
            }
        });
    }
}
