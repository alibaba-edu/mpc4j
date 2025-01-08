package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.decomposer.Cgp20PermutationDecomposer;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.*;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstFactory.PstType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.cgp20.Cgp20PstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.lll24.Lll24PstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Partial Share Translation tests.
 *
 * @author Feng Han
 * @date 2024/8/6
 */
@RunWith(Parameterized.class)
public class PstTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PstTest.class);
    /**
     * default each num, we select an odd number and not with the format 2^k
     */
    private static final int DEFAULT_EACH_NUM = 16;
    /**
     * large each num
     */
    private static final int LARGE_EACH_NUM = 1024;
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

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // LLL24
        configurations.add(new Object[]{
            PstType.LLL24.name(), new Lll24PstConfig.Builder(false).build(),
        });
        // CGP20
        configurations.add(new Object[]{
            PstType.CGP20.name(), new Cgp20PstConfig.Builder(false).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PstConfig config;

    public PstTest(String name, PstConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testDefault() {
        for (int batchNum = 1; batchNum < DEFAULT_BATCH_NUM; batchNum++) {
            for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum *= 2) {
                int[][] piArray = randomPiArray(batchNum, eachNum, true);
                testPto(piArray, DEFAULT_BYTE_LENGTH, false, true);
                piArray = randomPiArray(batchNum, eachNum, false);
                testPto(piArray, DEFAULT_BYTE_LENGTH, false, false);
            }
        }
    }

    @Test
    public void testDefaultParallel() {
        for (int batchNum = 1; batchNum < DEFAULT_BATCH_NUM; batchNum++) {
            for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum *= 2) {
                int[][] piArray = randomPiArray(batchNum, eachNum, true);
                testPto(piArray, DEFAULT_BYTE_LENGTH, true, true);
                piArray = randomPiArray(batchNum, eachNum, false);
                testPto(piArray, DEFAULT_BYTE_LENGTH, true, false);
            }
        }
    }

    @Test
    public void testLargeEachNum() {
        for (int batchNum = 1; batchNum < DEFAULT_BATCH_NUM; batchNum++) {
            int eachNum = LARGE_EACH_NUM;
            int[][] piArray = randomPiArray(batchNum, eachNum, true);
            testPto(piArray, DEFAULT_BYTE_LENGTH, false, true);
            piArray = randomPiArray(batchNum, eachNum, false);
            testPto(piArray, DEFAULT_BYTE_LENGTH, false, false);
        }
    }

    @Test
    public void testLargeEachNumParallel() {
        for (int batchNum = 1; batchNum < DEFAULT_BATCH_NUM; batchNum++) {
            int eachNum = LARGE_EACH_NUM;
            int[][] piArray = randomPiArray(batchNum, eachNum, true);
            testPto(piArray, DEFAULT_BYTE_LENGTH, true, true);
            piArray = randomPiArray(batchNum, eachNum, false);
            testPto(piArray, DEFAULT_BYTE_LENGTH, true, false);
        }
    }

    @Test
    public void testLargeBatchNum() {
        int batchNum = LARGE_BATCH_NUM;
        for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum *= 2) {
            LOGGER.info("eachNum : {}", eachNum);
            int[][] piArray = randomPiArray(batchNum, eachNum, true);
            testPto(piArray, DEFAULT_BYTE_LENGTH, false, true);
            piArray = randomPiArray(batchNum, eachNum, false);
            testPto(piArray, DEFAULT_BYTE_LENGTH, false, false);
        }
    }

    @Test
    public void testLargeBatchNumParallel() {
        int batchNum = LARGE_BATCH_NUM;
        for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum *= 2) {
            int[][] piArray = randomPiArray(batchNum, eachNum, true);
            testPto(piArray, DEFAULT_BYTE_LENGTH, true, true);
            piArray = randomPiArray(batchNum, eachNum, false);
            testPto(piArray, DEFAULT_BYTE_LENGTH, true, false);
        }
    }

    @Test
    public void testLargeByteLength() {
        for (int byteLength = 1; byteLength < DEFAULT_BYTE_LENGTH / 2; byteLength++) {
            int[][] piArray = randomPiArray(DEFAULT_BATCH_NUM, DEFAULT_EACH_NUM, true);
            testPto(piArray, byteLength, false, true);
            piArray = randomPiArray(DEFAULT_BATCH_NUM, DEFAULT_EACH_NUM, false);
            testPto(piArray, byteLength, false, false);
        }
    }

    private void testPto(int[][] piArray, int byteLength, boolean parallel, boolean isLeft) {
        PstSender sender = PstFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PstReceiver receiver = PstFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int batchNum = piArray.length;
            int eachNum = piArray[0].length;
            PstSenderThread senderThread = new PstSenderThread(sender, piArray, byteLength, isLeft);
            PstReceiverThread receiverThread = new PstReceiverThread(receiver, batchNum, eachNum, byteLength, isLeft);
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
                for (int eachNum = 2; eachNum <= DEFAULT_EACH_NUM; eachNum *= 2) {
                    PstSender sender = PstFactory.createSender(firstRpc, secondRpc.ownParty(), config);
                    PstReceiver receiver = PstFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
                    LOGGER.info("-----test {} (precompute) start, (batch = {}, each = {}) -----",
                        sender.getPtoDesc().getPtoName(), batchNum, eachNum
                    );
                    int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
                    sender.setTaskId(randomTaskId);
                    receiver.setTaskId(randomTaskId);
                    int[][] piArray = randomPiArray(batchNum, eachNum, true);
                    byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
                    CotSenderOutput preSenderOutput = CotSenderOutput.createRandom(
                        PstFactory.getPrecomputeNum(config, batchNum, eachNum), delta, SECURE_RANDOM
                    );
                    CotReceiverOutput preReceiverOutput = CotReceiverOutput.createRandom(preSenderOutput, SECURE_RANDOM);
                    PstSenderThread senderThread = new PstSenderThread(
                        sender, piArray, DEFAULT_BYTE_LENGTH, preReceiverOutput, true
                    );
                    PstReceiverThread receiverThread = new PstReceiverThread(
                        receiver, batchNum, eachNum, DEFAULT_BYTE_LENGTH, preSenderOutput, true
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

    private int[][] randomPiArray(int batchNum, int eachNum, boolean isLeft) {
        if(eachNum == 2){
            return IntStream.range(0, batchNum)
                .mapToObj(i -> PermutationNetworkUtils.randomPermutation(eachNum, SECURE_RANDOM))
                .toArray(int[][]::new);
        }
        // batchNum + 1为了避免生成的网络中只有一层
        int log2 = LongUtils.ceilLog2(batchNum + 1);
        int allNum = (1 << log2) * eachNum;
        int[] pi = PermutationNetworkUtils.randomPermutation(allNum, SECURE_RANDOM);
        Cgp20PermutationDecomposer permutationDecomposer = new Cgp20PermutationDecomposer(allNum, eachNum);
        permutationDecomposer.setPermutation(pi);
        int randomIndex = SECURE_RANDOM.nextInt(permutationDecomposer.getD() / 2);
        int halfIndex = permutationDecomposer.getD() / 2;
        if (isLeft) {
            return Arrays.copyOf(permutationDecomposer.getSubPermutations()[randomIndex], batchNum);
        } else {
            return Arrays.copyOf(permutationDecomposer.getSubPermutations()[randomIndex + halfIndex + 1], batchNum);
        }
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
