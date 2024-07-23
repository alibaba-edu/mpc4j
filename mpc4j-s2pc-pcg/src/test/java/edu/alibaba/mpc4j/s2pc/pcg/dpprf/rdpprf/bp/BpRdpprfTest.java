package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory.BpRdpprfType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.ywl20.Ywl20BpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
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
 * batch-point RDPPRF tests.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
@RunWith(Parameterized.class)
public class BpRdpprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(BpRdpprfTest.class);
    /**
     * default n, which is not even, and not in format 2^k
     */
    private static final int DEFAULT_EACH_NUM = 15;
    /**
     * large n
     */
    private static final int LARGE_EACH_NUM = 1 << 16;
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
        configurations.add(new Object[]{
            BpRdpprfType.YWL20.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new Ywl20BpRdpprfConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20 (malicious)
        configurations.add(new Object[]{
            BpRdpprfType.YWL20.name() + " (" + SecurityModel.MALICIOUS + ")",
            new Ywl20BpRdpprfConfig.Builder(SecurityModel.MALICIOUS).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final BpRdpprfConfig config;

    public BpRdpprfTest(String name, BpRdpprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testFirstAlpha() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> 0)
            .toArray();
        testPto(alphaArray, DEFAULT_EACH_NUM, false);
    }

    @Test
    public void testLastAlpha() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> DEFAULT_EACH_NUM - 1)
            .toArray();
        testPto(alphaArray, DEFAULT_EACH_NUM, false);
    }

    @Test
    public void test1AlphaBound() {
        int eachNum = 1;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> 0)
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test2AlphaBound() {
        int eachNum = 2;
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test1Batch() {
        int batchNum = 1;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_EACH_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_EACH_NUM, false);
    }

    @Test
    public void test2Batch() {
        int batchNum = 2;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_EACH_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_EACH_NUM, false);
    }

    @Test
    public void testDefault() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_EACH_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_EACH_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_EACH_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_EACH_NUM, true);
    }

    @Test
    public void testLargeBatchNum() {
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_EACH_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_EACH_NUM, false);
    }

    @Test
    public void testParallelLargeBatchNum() {
        int[] alphaArray = IntStream.range(0, LARGE_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(DEFAULT_EACH_NUM))
            .toArray();
        testPto(alphaArray, DEFAULT_EACH_NUM, true);
    }

    @Test
    public void testLargeEachNum() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_EACH_NUM))
            .toArray();
        testPto(alphaArray, LARGE_EACH_NUM, false);
    }

    @Test
    public void testParallelLargeEachNum() {
        int[] alphaArray = IntStream.range(0, DEFAULT_BATCH_NUM)
            .map(mIndex -> SECURE_RANDOM.nextInt(LARGE_EACH_NUM))
            .toArray();
        testPto(alphaArray, LARGE_EACH_NUM, true);
    }

    private void testPto(int[] alphaArray, int alphaBound, boolean parallel) {
        BpRdpprfSender sender = BpRdpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BpRdpprfReceiver receiver = BpRdpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int batchNum = alphaArray.length;
            BpRdpprfSenderThread senderThread = new BpRdpprfSenderThread(sender, batchNum, alphaBound);
            BpRdpprfReceiverThread receiverThread = new BpRdpprfReceiverThread(receiver, alphaArray, alphaBound);
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
            BpRdpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            BpRdpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, alphaBound, senderOutput, receiverOutput);
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
        BpRdpprfSender sender = BpRdpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BpRdpprfReceiver receiver = BpRdpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int batchNum = DEFAULT_BATCH_NUM;
        int alphaBound = DEFAULT_EACH_NUM;
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(mIndex -> SECURE_RANDOM.nextInt(batchNum))
            .toArray();
        CotSenderOutput preSenderOutput = CotSenderOutput.createRandom(
            BpRdpprfFactory.getPrecomputeNum(config, batchNum, alphaBound), delta, SECURE_RANDOM
        );
        CotReceiverOutput preReceiverOutput = CotReceiverOutput.createRandom(preSenderOutput, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            BpRdpprfSenderThread senderThread = new BpRdpprfSenderThread(sender, batchNum, alphaBound, preSenderOutput);
            BpRdpprfReceiverThread receiverThread = new BpRdpprfReceiverThread(
                receiver, alphaArray, alphaBound, preReceiverOutput
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
            BpRdpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            BpRdpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, alphaBound, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int batchNum, int eachNum, BpRdpprfSenderOutput senderOutput, BpRdpprfReceiverOutput receiverOutput) {
        Assert.assertEquals(batchNum, senderOutput.getBatchNum());
        Assert.assertEquals(batchNum, receiverOutput.getBatchNum());
        Assert.assertEquals(eachNum, senderOutput.getEachNum());
        Assert.assertEquals(eachNum, receiverOutput.getEachNum());
        // verify each single-point DPPRF outputs
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            SpRdpprfSenderOutput spRdpprfSenderOutput = senderOutput.get(batchIndex);
            SpRdpprfReceiverOutput spRdpprfReceiverOutput = receiverOutput.get(batchIndex);
            byte[][] prfKey = spRdpprfSenderOutput.getV0Array();
            byte[][] pprfKey = spRdpprfReceiverOutput.getV1Array();
            IntStream.range(0, eachNum).forEach(index -> {
                if (index == spRdpprfReceiverOutput.getAlpha()) {
                    Assert.assertNull(pprfKey[index]);
                } else {
                    Assert.assertArrayEquals(prfKey[index], pprfKey[index]);
                }
            });
        });
    }
}
