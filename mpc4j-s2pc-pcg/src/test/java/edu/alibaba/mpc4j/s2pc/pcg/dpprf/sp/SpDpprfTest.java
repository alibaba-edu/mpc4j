package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.ywl20.Ywl20SpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory.SpDpprfType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
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
public class SpDpprfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpDpprfTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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
        configurations.add(new Object[] {
            SpDpprfType.YWL20.name() + " (semi-honest)", new Ywl20SpDpprfConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20 (malicious)
        configurations.add(new Object[] {
            SpDpprfType.YWL20.name() + " (malicious)", new Ywl20SpDpprfConfig.Builder(SecurityModel.MALICIOUS).build(),
        });

        return configurations;
    }

    /**
     * the sender RPC
     */
    private final Rpc senderRpc;
    /**
     * the receiver RPC
     */
    private final Rpc receiverRpc;
    /**
     * config
     */
    private final SpDpprfConfig config;

    public SpDpprfTest(String name, SpDpprfConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Before
    public void connect() {
        senderRpc.connect();
        receiverRpc.connect();
    }

    @After
    public void disconnect() {
        senderRpc.disconnect();
        receiverRpc.disconnect();
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
        SpDpprfSender sender = SpDpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SpDpprfReceiver receiver = SpDpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            SpDpprfSenderThread senderThread = new SpDpprfSenderThread(sender, alphaBound);
            SpDpprfReceiverThread receiverThread = new SpDpprfReceiverThread(receiver, alpha, alphaBound);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long senderByteLength = senderRpc.getSendByteLength();
            long receiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            // verify
            SpDpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            SpDpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(alphaBound, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    @Test
    public void testPrecompute() {
        SpDpprfSender sender = SpDpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SpDpprfReceiver receiver = SpDpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long senderByteLength = senderRpc.getSendByteLength();
            long receiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            // verify
            SpDpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            SpDpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(alphaBound, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
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
