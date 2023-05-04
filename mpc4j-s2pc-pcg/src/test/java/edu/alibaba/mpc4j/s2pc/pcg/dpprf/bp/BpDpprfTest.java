package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory.BpDpprfType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.ywl20.Ywl20BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
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
 * batch-point DPPRF tests.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
@RunWith(Parameterized.class)
public class BpDpprfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BpDpprfTest.class);
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
    /**
     * default batch num, the batch num is not even, and not in format 2^k
     */
    private static final int DEFAULT_BATCH_NUM = 9;
    /**
     * large batch num
     */
    private static final int LARGE_BATCH_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // YWL20 (semi-honest)
        configurations.add(new Object[] {
            BpDpprfType.YWL20.name() + " (semi-honest)", new Ywl20BpDpprfConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20 (malicious)
        configurations.add(new Object[] {
            BpDpprfType.YWL20.name() + " (malicious)", new Ywl20BpDpprfConfig.Builder(SecurityModel.MALICIOUS).build(),
        });

        return configurations;
    }

    /**
     * the sender
     */
    private final Rpc senderRpc;
    /**
     * the receiver
     */
    private final Rpc receiverRpc;
    /**
     * config
     */
    private final BpDpprfConfig config;

    public BpDpprfTest(String name, BpDpprfConfig config) {
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
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> 0)
            .toArray();
        testPto(alphaArray, DEFAULT_ALPHA_BOUND, false);
    }

    @Test
    public void testLastAlpha() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> DEFAULT_ALPHA_BOUND - 1)
            .toArray();
        testPto(alphaArray, DEFAULT_ALPHA_BOUND, false);
    }

    @Test
    public void test1AlphaBound() {
        int alphaBound = 1;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(alphaBound))
            .toArray();
        testPto(alphaArray, alphaBound, false);
    }

    @Test
    public void test2AlphaBound() {
        int alphaBound = 2;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(alphaBound))
            .toArray();
        testPto(alphaArray, alphaBound, false);
    }

    @Test
    public void test1Batch() {
        int batchNum = 1;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(alphaArray, DEFAULT_ALPHA_BOUND, false);
    }

    @Test
    public void test2Batch() {
        int batchNum = 2;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(alphaArray, DEFAULT_ALPHA_BOUND, false);
    }

    @Test
    public void testDefault() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(alphaArray, DEFAULT_ALPHA_BOUND, false);
    }

    @Test
    public void testParallelDefault() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(alphaArray, DEFAULT_ALPHA_BOUND, true);
    }

    @Test
    public void testLargeBatchNum() {
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(alphaArray, DEFAULT_ALPHA_BOUND, false);
    }

    @Test
    public void testParallelLargeBatchNum() {
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(alphaArray, DEFAULT_ALPHA_BOUND, true);
    }

    @Test
    public void testLargeAlphaBound() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_ALPHA_BOUND))
            .toArray();
        testPto(alphaArray, LARGE_ALPHA_BOUND, false);
    }

    @Test
    public void testParallelLargeAlphaBound() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_ALPHA_BOUND))
            .toArray();
        testPto(alphaArray, LARGE_ALPHA_BOUND, true);
    }

    private void testPto(int[] alphaArray, int alphaBound, boolean parallel) {
        BpDpprfSender sender = BpDpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BpDpprfReceiver receiver = BpDpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int batchNum = alphaArray.length;
            BpDpprfSenderThread senderThread = new BpDpprfSenderThread(sender, batchNum, alphaBound);
            BpDpprfReceiverThread receiverThread = new BpDpprfReceiverThread(receiver, alphaArray, alphaBound);
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
            BpDpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            BpDpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, alphaBound, senderOutput, receiverOutput);
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
        BpDpprfSender sender = BpDpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BpDpprfReceiver receiver = BpDpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int batchNum = DEFAULT_BATCH_NUM;
        int alphaBound = DEFAULT_ALPHA_BOUND;
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(delta);
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(batchNum))
            .toArray();
        CotSenderOutput preSenderOutput = CotTestUtils.genSenderOutput(
            BpDpprfFactory.getPrecomputeNum(config, batchNum, alphaBound), delta, SECURE_RANDOM
        );
        CotReceiverOutput preReceiverOutput = CotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            BpDpprfSenderThread senderThread = new BpDpprfSenderThread(sender, batchNum, alphaBound, preSenderOutput);
            BpDpprfReceiverThread receiverThread = new BpDpprfReceiverThread(
                receiver, alphaArray, alphaBound, preReceiverOutput
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
            BpDpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            BpDpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, alphaBound, senderOutput, receiverOutput);
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

    private void assertOutput(int batchNum, int alphaBound, BpDpprfSenderOutput senderOutput, BpDpprfReceiverOutput receiverOutput) {
        Assert.assertEquals(batchNum, senderOutput.getNum());
        Assert.assertEquals(batchNum, receiverOutput.getNum());
        Assert.assertEquals(alphaBound, senderOutput.getAlphaBound());
        Assert.assertEquals(alphaBound, receiverOutput.getAlphaBound());
        // verify each single-point DPPRF outputs
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            SpDpprfSenderOutput spDpprfSenderOutput = senderOutput.getSpDpprfSenderOutput(batchIndex);
            SpDpprfReceiverOutput spDpprfReceiverOutput = receiverOutput.getSpDpprfReceiverOutput(batchIndex);
            byte[][] prfKey = spDpprfSenderOutput.getPrfKeys();
            byte[][] pprfKey = spDpprfReceiverOutput.getPprfKeys();
            IntStream.range(0, alphaBound).forEach(index -> {
                if (index == spDpprfReceiverOutput.getAlpha()) {
                    Assert.assertNull(pprfKey[index]);
                } else {
                    Assert.assertArrayEquals(prfKey[index], pprfKey[index]);
                }
            });
        });
    }
}
