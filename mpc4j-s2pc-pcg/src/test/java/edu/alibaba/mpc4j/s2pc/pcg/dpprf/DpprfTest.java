package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfFactory.DpprfType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.ywl20.Ywl20DpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
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
 * DPPRF协议测试。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
@RunWith(Parameterized.class)
public class DpprfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DpprfTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认α上界，设置为既不是偶数、也不是2^k格式的上界
     */
    private static final int DEFAULT_ALPHA_BOUND = 15;
    /**
     * 较大α上界
     */
    private static final int LARGE_ALPHA_BOUND = 1 << 16;
    /**
     * 默认批处理数量，设置为既不是偶数、也不是2^k格式的数量
     */
    private static final int DEFAULT_BATCH_NUM = 9;
    /**
     * 较大批处理数量
     */
    private static final int LARGE_BATCH_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // YWL20 (semi-honest)
        configurationParams.add(new Object[] {
            DpprfType.YWL20.name() + " (semi-honest)", new Ywl20DpprfConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20 (malicious)
        configurationParams.add(new Object[] {
            DpprfType.YWL20.name() + " (malicious)", new Ywl20DpprfConfig.Builder(SecurityModel.MALICIOUS).build(),
        });

        return configurationParams;
    }

    /**
     * 发送方
     */
    private final Rpc senderRpc;
    /**
     * 接收方
     */
    private final Rpc receiverRpc;
    /**
     * 协议类型
     */
    private final DpprfConfig config;

    public DpprfTest(String name, DpprfConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void testFirstAlpha() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> 0)
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_ALPHA_BOUND);
    }

    @Test
    public void testLastAlpha() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> DEFAULT_ALPHA_BOUND - 1)
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_ALPHA_BOUND);
    }

    @Test
    public void test1AlphaBound() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int alphaBound = 1;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(alphaBound))
            .toArray();
        testPto(sender, receiver, alphaArray, alphaBound);
    }

    @Test
    public void test2AlphaBound() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int alphaBound = 2;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(alphaBound))
            .toArray();
        testPto(sender, receiver, alphaArray, alphaBound);
    }

    @Test
    public void test1Batch() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int batchNum = 1;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_ALPHA_BOUND);
    }

    @Test
    public void test2Batch() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int batchNum = 2;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_ALPHA_BOUND);
    }

    @Test
    public void testDefault() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_ALPHA_BOUND);
    }

    @Test
    public void testParallelDefault() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_ALPHA_BOUND);
    }

    @Test
    public void testLargeBatchNum() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_ALPHA_BOUND);
    }

    @Test
    public void testParallelLargeBatchNum() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_ALPHA_BOUND))
            .toArray();
        testPto(sender, receiver, alphaArray, DEFAULT_ALPHA_BOUND);
    }

    @Test
    public void testLargeAlphaBound() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_ALPHA_BOUND))
            .toArray();
        testPto(sender, receiver, alphaArray, LARGE_ALPHA_BOUND);
    }

    @Test
    public void testParallelLargeAlphaBound() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_ALPHA_BOUND))
            .toArray();
        testPto(sender, receiver, alphaArray, LARGE_ALPHA_BOUND);
    }

    private void testPto(DpprfSender sender, DpprfReceiver receiver, int[] alphaArray, int alphaBound) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int batchNum = alphaArray.length;
            DpprfSenderThread senderThread = new DpprfSenderThread(sender, batchNum, alphaBound);
            DpprfReceiverThread receiverThread = new DpprfReceiverThread(receiver, alphaArray, alphaBound);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
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
            DpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            DpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(batchNum, alphaBound, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPrecompute() {
        DpprfSender sender = DpprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        DpprfReceiver receiver = DpprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int batchNum = DEFAULT_BATCH_NUM;
        int alphaBound = DEFAULT_ALPHA_BOUND;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            int[] alphaArray = IntStream.range(0, batchNum)
                .map(mIndex -> SECURE_RANDOM.nextInt(batchNum))
                .toArray();
            CotSenderOutput preSenderOutput = CotTestUtils.genSenderOutput(
                DpprfFactory.getPrecomputeNum(config, batchNum, alphaBound), delta, SECURE_RANDOM
            );
            CotReceiverOutput preReceiverOutput = CotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
            DpprfSenderThread senderThread = new DpprfSenderThread(sender, batchNum, alphaBound, preSenderOutput);
            DpprfReceiverThread receiverThread = new DpprfReceiverThread(
                receiver, alphaArray, alphaBound, preReceiverOutput
            );
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
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
            DpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            DpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(batchNum, alphaBound, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int batchNum, int alphaBound,
                              DpprfSenderOutput senderOutput, DpprfReceiverOutput receiverOutput) {
        Assert.assertEquals(batchNum, senderOutput.getBatchNum());
        Assert.assertEquals(batchNum, receiverOutput.getBatchNum());
        Assert.assertEquals(alphaBound, senderOutput.getAlphaBound());
        Assert.assertEquals(alphaBound, receiverOutput.getAlphaBound());
        // 验证各个子结果
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            byte[][] prfKey = senderOutput.getPrfOutputArray(batchIndex);
            byte[][] pprfKey = receiverOutput.getPprfOutputArray(batchIndex);
            IntStream.range(0, alphaBound).forEach(index -> {
                if (index == receiverOutput.getAlpha(batchIndex)) {
                    Assert.assertNull(pprfKey[index]);
                } else {
                    Assert.assertArrayEquals(prfKey[index], pprfKey[index]);
                }
            });
        });
    }
}
