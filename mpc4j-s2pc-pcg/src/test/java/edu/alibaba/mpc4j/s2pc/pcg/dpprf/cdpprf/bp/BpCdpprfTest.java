package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory.BpCdpprfType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.gyw23.Gyw23BpCdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfSenderOutput;
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
 * BP-CDPPRF tests.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
@RunWith(Parameterized.class)
public class BpCdpprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(BpCdpprfTest.class);
    /**
     * default each num with the format 2^k
     */
    private static final int DEFAULT_EACH_NUM = 1 << 4;
    /**
     * large each num
     */
    private static final int LARGE_EACH_NUM = 1 << 16;
    /**
     * default batch num, we select an odd number and not with the format 2^k
     */
    private static final int DEFAULT_BATCH_NUM = 9;
    /**
     * large batch num
     */
    private static final int LARGE_BATCH_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // GYW23
        configurations.add(new Object[]{
            BpCdpprfType.GYW23.name(), new Gyw23BpCdpprfConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final BpCdpprfConfig config;

    public BpCdpprfTest(String name, BpCdpprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testFirstAlpha() {
        //noinspection UnnecessaryLocalVariable
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> 0)
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testLastAlpha() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> eachNum - 1)
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test1EachNum() {
        int eachNum = 1;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test2EachNum() {
        int eachNum = 2;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test1BatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = 1;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void test2BatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = 2;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testDefault() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testParallelDefault() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, true);
    }

    @Test
    public void testLargeBatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = LARGE_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testParallelLargeBatchNum() {
        int eachNum = DEFAULT_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = LARGE_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, true);
    }

    @Test
    public void testLargeEachNum() {
        int eachNum = LARGE_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, false);
    }

    @Test
    public void testParallelLargeEachNum() {
        int eachNum = LARGE_EACH_NUM;
        //noinspection UnnecessaryLocalVariable
        int batchNum = DEFAULT_BATCH_NUM;
        int[] alphaArray = IntStream.range(0, batchNum)
            .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
            .toArray();
        testPto(alphaArray, eachNum, true);
    }

    private void testPto(int[] alphaArray, int eachNum, boolean parallel) {
        BpCdpprfSender sender = BpCdpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BpCdpprfReceiver receiver = BpCdpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int batchNum = alphaArray.length;
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            BpCdpprfSenderThread senderThread = new BpCdpprfSenderThread(sender, delta, batchNum, eachNum);
            BpCdpprfReceiverThread receiverThread = new BpCdpprfReceiverThread(receiver, alphaArray, eachNum);
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
            BpCdpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            BpCdpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, eachNum, senderOutput, receiverOutput);
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
        BpCdpprfSender sender = BpCdpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BpCdpprfReceiver receiver = BpCdpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int eachNum = DEFAULT_EACH_NUM;
        int batchNum = DEFAULT_BATCH_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] initDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(initDelta);
            byte[] actualDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(actualDelta);
            int[] alphaArray = IntStream.range(0, batchNum)
                .map(alphaIndex -> SECURE_RANDOM.nextInt(eachNum))
                .toArray();
            CotSenderOutput preSenderOutput = CotSenderOutput.createRandom(
                BpCdpprfFactory.getPrecomputeNum(config, batchNum, eachNum), actualDelta, SECURE_RANDOM
            );
            CotReceiverOutput preReceiverOutput = CotReceiverOutput.createRandom(preSenderOutput, SECURE_RANDOM);
            BpCdpprfSenderThread senderThread = new BpCdpprfSenderThread(
                sender, initDelta, alphaArray.length, eachNum, preSenderOutput
            );
            BpCdpprfReceiverThread receiverThread = new BpCdpprfReceiverThread(
                receiver, alphaArray, eachNum, preReceiverOutput
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
            BpCdpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            BpCdpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchNum, eachNum, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int batchNum, int eachNum,
                              BpCdpprfSenderOutput senderOutput, BpCdpprfReceiverOutput receiverOutput) {
        Assert.assertEquals(batchNum, senderOutput.getBatchNum());
        Assert.assertEquals(batchNum, receiverOutput.getBatchNum());
        Assert.assertEquals(eachNum, senderOutput.getEachNum());
        Assert.assertEquals(eachNum, receiverOutput.getEachNum());
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            SpCdpprfSenderOutput eachSenderOutput = senderOutput.get(batchIndex);
            SpCdpprfReceiverOutput eachReceiverOutput = receiverOutput.get(batchIndex);
            Assert.assertEquals(eachNum, eachSenderOutput.getNum());
            Assert.assertEquals(eachNum, eachReceiverOutput.getNum());
            byte[] actualDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            IntStream.range(0, eachNum).forEach(index -> {
                if (index == eachReceiverOutput.getAlpha()) {
                    Assert.assertNull(eachReceiverOutput.getV1(index));
                } else {
                    Assert.assertArrayEquals(eachSenderOutput.getV0(index), eachReceiverOutput.getV1(index));
                }
                BlockUtils.xori(actualDelta, eachSenderOutput.getV0(index));
            });
            Assert.assertArrayEquals(senderOutput.getDelta(), actualDelta);
        });
    }
}
