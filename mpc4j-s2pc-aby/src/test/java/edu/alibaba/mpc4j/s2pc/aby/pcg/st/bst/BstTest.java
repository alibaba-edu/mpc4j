package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory.BstType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20.Cgp20BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24.Lll24BstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import org.apache.commons.lang3.ArrayUtils;
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
 * Batched Share Translation tests.
 *
 * @author Weiran Liu
 * @date 2024/4/24
 */
@RunWith(Parameterized.class)
public class BstTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(BstTest.class);
    /**
     * default each num, we select an odd number and not with the format 2^k
     */
    private static final int DEFAULT_EACH_NUM = 5;
    /**
     * large each num
     */
    private static final int LARGE_EACH_NUM = (1 << 10) + 1;
    /**
     * default batch num, we select an odd number and not with the format 2^k
     */
    private static final int DEFAULT_BATCH_NUM = 5;
    /**
     * large batch num
     */
    private static final int LARGE_BATCH_NUM = 1 << 8;
    /**
     * default element byte length
     */
    private static final int DEFAULT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * short element byte length
     */
    private static final int SHORT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH / 2 - 1;
    /**
     * large element byte length
     */
    private static final int LARGE_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH * 2 + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // LLL24
        configurations.add(new Object[]{
            BstType.LLL24.name(), new Lll24BstConfig.Builder().build(),
        });
        // CGP20
        configurations.add(new Object[]{
            BstType.CGP20.name(), new Cgp20BstConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final BstConfig config;

    public BstTest(String name, BstConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testDefault() {
        for (int batchNum = 1; batchNum < DEFAULT_BATCH_NUM; batchNum++) {
            for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum++) {
                int[][] piArray = randomPiArray(batchNum, eachNum);
                testPto(piArray, DEFAULT_BYTE_LENGTH, false);
            }
        }
    }

    @Test
    public void testDefaultParallel() {
        for (int batchNum = 1; batchNum < DEFAULT_BATCH_NUM; batchNum++) {
            for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum++) {
                int[][] piArray = randomPiArray(batchNum, eachNum);
                testPto(piArray, DEFAULT_BYTE_LENGTH, true);
            }
        }
    }

    @Test
    public void testShortByteLength() {
        int[][] piArray = randomPiArray(DEFAULT_BATCH_NUM, DEFAULT_EACH_NUM);
        testPto(piArray, SHORT_BYTE_LENGTH, true);
    }

    @Test
    public void testLargeByteLength() {
        int[][] piArray = randomPiArray(DEFAULT_BATCH_NUM, DEFAULT_EACH_NUM);
        testPto(piArray, LARGE_BYTE_LENGTH, true);
    }

    @Test
    public void testLargeEachNum() {
        for (int batchNum = 1; batchNum < DEFAULT_BATCH_NUM; batchNum++) {
            //noinspection UnnecessaryLocalVariable
            int eachNum = LARGE_EACH_NUM;
            int[][] piArray = randomPiArray(batchNum, eachNum);
            testPto(piArray, DEFAULT_BYTE_LENGTH, false);
        }
    }

    @Test
    public void testLargeEachNumParallel() {
        for (int batchNum = 1; batchNum < DEFAULT_BATCH_NUM; batchNum++) {
            //noinspection UnnecessaryLocalVariable
            int eachNum = LARGE_EACH_NUM;
            int[][] piArray = randomPiArray(batchNum, eachNum);
            testPto(piArray, DEFAULT_BYTE_LENGTH, true);
        }
    }

    @Test
    public void testLargeBatchNum() {
        //noinspection UnnecessaryLocalVariable
        int batchNum = LARGE_BATCH_NUM;
        for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum++) {
            int[][] piArray = randomPiArray(batchNum, eachNum);
            testPto(piArray, DEFAULT_BYTE_LENGTH, false);
        }
    }

    @Test
    public void testLargeBatchNumParallel() {
        //noinspection UnnecessaryLocalVariable
        int batchNum = LARGE_BATCH_NUM;
        for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum++) {
            int[][] piArray = randomPiArray(batchNum, eachNum);
            testPto(piArray, DEFAULT_BYTE_LENGTH, true);
        }
    }

    private void testPto(int[][] piArray, int byteLength, boolean parallel) {
        BstSender sender = BstFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BstReceiver receiver = BstFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int batchNum = piArray.length;
            int eachNum = piArray[0].length;
            BstSenderThread senderThread = new BstSenderThread(sender, piArray, byteLength);
            BstReceiverThread receiverThread = new BstReceiverThread(receiver, batchNum, eachNum, byteLength);
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
            BstSenderOutput senderOutput = senderThread.getSenderOutput();
            BstReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(piArray, senderOutput, receiverOutput);
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
        try {
            for (int batchNum = 1; batchNum < DEFAULT_BATCH_NUM; batchNum++) {
                for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum++) {
                    BstSender sender = BstFactory.createSender(firstRpc, secondRpc.ownParty(), config);
                    BstReceiver receiver = BstFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
                    LOGGER.info("-----test {} (precompute) start, (batch = {}, each = {}) -----",
                        sender.getPtoDesc().getPtoName(), batchNum, eachNum
                    );
                    int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
                    sender.setTaskId(randomTaskId);
                    receiver.setTaskId(randomTaskId);
                    int[][] piArray = randomPiArray(batchNum, eachNum);
                    byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
                    CotSenderOutput preSenderOutput = CotSenderOutput.createRandom(
                        BstFactory.getPrecomputeNum(config, batchNum, eachNum), delta, SECURE_RANDOM
                    );
                    CotReceiverOutput preReceiverOutput = CotReceiverOutput.createRandom(preSenderOutput, SECURE_RANDOM);
                    BstSenderThread senderThread = new BstSenderThread(
                        sender, piArray, DEFAULT_BYTE_LENGTH, preReceiverOutput
                    );
                    BstReceiverThread receiverThread = new BstReceiverThread(
                        receiver, batchNum, eachNum, DEFAULT_BYTE_LENGTH, preSenderOutput
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
                    BstSenderOutput senderOutput = senderThread.getSenderOutput();
                    BstReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
                    assertOutput(piArray, senderOutput, receiverOutput);
                    printAndResetRpc(time);
                    LOGGER.info("-----test {} (end), (batch = {}, each = {}) -----",
                        sender.getPtoDesc().getPtoName(), batchNum, eachNum
                    );
                    // destroy
                    new Thread(sender::destroy).start();
                    new Thread(receiver::destroy).start();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int[][] randomPiArray(int batchNum, int eachNum) {
        return IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                int[] pi = IntStream.range(0, eachNum).toArray();
                ArrayUtils.shuffle(pi, SECURE_RANDOM);
                return pi;
            })
            .toArray(int[][]::new);
    }

    private void assertOutput(int[][] piArray, BstSenderOutput senderOutput, BstReceiverOutput receiverOutput) {
        Assert.assertEquals(piArray.length, senderOutput.getBatchNum());
        for (int batchIndex = 0; batchIndex < piArray.length; batchIndex++) {
            SstSenderOutput eachSenderOutput = senderOutput.get(batchIndex);
            SstReceiverOutput eachReceiverOutput = receiverOutput.get(batchIndex);
            int[] pi = piArray[batchIndex];
            int num = pi.length;
            Assert.assertArrayEquals(pi, eachSenderOutput.getPi());
            Assert.assertEquals(num, eachSenderOutput.getNum());
            Assert.assertEquals(num, eachReceiverOutput.getNum());
            // Δ = π(a) ⊕ b
            byte[][] a = eachReceiverOutput.getAs();
            byte[][] b = eachReceiverOutput.getBs();
            byte[][] expectDeltas = eachSenderOutput.getDeltas();
            byte[][] pa = PermutationNetworkUtils.permutation(pi, a);
            byte[][] actualDeltas = IntStream.range(0, num)
                .mapToObj(i -> BytesUtils.xor(pa[i], b[i]))
                .toArray(byte[][]::new);
            IntStream.range(0, num).forEach(i -> Assert.assertArrayEquals(expectDeltas[i], actualDeltas[i]));
        }
    }
}
