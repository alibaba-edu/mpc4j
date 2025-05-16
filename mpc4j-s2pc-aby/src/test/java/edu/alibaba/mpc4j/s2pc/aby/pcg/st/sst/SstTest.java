package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstFactory.SstType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.cgp20.Cgp20SstConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.lll24.Lll24SstConfig;
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
 * Single Share Translation tests.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
@RunWith(Parameterized.class)
public class SstTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SstTest.class);
    /**
     * default num, we select an odd number and not with the format 2^k
     */
    private static final int DEFAULT_NUM = 5;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 10) + 1;
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
            SstType.LLL24.name(), new Lll24SstConfig.Builder().build(),
        });
        // CGP20
        configurations.add(new Object[]{
            SstType.CGP20.name(), new Cgp20SstConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final SstConfig config;

    public SstTest(String name, SstConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2N() {
        int[] pi0 = new int[]{0, 1};
        testPto(pi0, DEFAULT_BYTE_LENGTH, false);
        int[] pi1 = new int[]{1, 0};
        testPto(pi1, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testConstantDefault() {
        int num = DEFAULT_NUM;
        int[] pi0 = IntStream.range(0, num).toArray();
        testPto(pi0, DEFAULT_BYTE_LENGTH, false);
        int[] pi1 = IntStream.range(0, num).map(i -> num - 1 - i).toArray();
        testPto(pi1, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testDefault() {
        for (int num = 2; num <= DEFAULT_NUM; num++) {
            int[] pi = PermutationNetworkUtils.randomPermutation(num, SECURE_RANDOM);
            testPto(pi, DEFAULT_BYTE_LENGTH, false);
        }
    }

    @Test
    public void testDefaultParallel() {
        for (int num = 2; num <= DEFAULT_NUM; num++) {
            int[] pi = PermutationNetworkUtils.randomPermutation(num, SECURE_RANDOM);
            testPto(pi, DEFAULT_BYTE_LENGTH, true);
        }
    }

    @Test
    public void testShortByteLength() {
        int[] pi = PermutationNetworkUtils.randomPermutation(DEFAULT_NUM, SECURE_RANDOM);
        testPto(pi, SHORT_BYTE_LENGTH, true);
    }

    @Test
    public void testLargeByteLength() {
        int[] pi = PermutationNetworkUtils.randomPermutation(DEFAULT_NUM, SECURE_RANDOM);
        testPto(pi, LARGE_BYTE_LENGTH, true);
    }

    @Test
    public void testLarge() {
        //noinspection UnnecessaryLocalVariable
        int num = LARGE_NUM;
        int[] pi = PermutationNetworkUtils.randomPermutation(num, SECURE_RANDOM);
        testPto(pi, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeParallel() {
        //noinspection UnnecessaryLocalVariable
        int num = LARGE_NUM;
        int[] pi = PermutationNetworkUtils.randomPermutation(num, SECURE_RANDOM);
        testPto(pi, DEFAULT_BYTE_LENGTH, true);
    }

    private void testPto(int[] pi, int byteLength, boolean parallel) {
        SstSender sender = SstFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SstReceiver receiver = SstFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int n = pi.length;
            SstSenderThread senderThread = new SstSenderThread(sender, pi, byteLength);
            SstReceiverThread receiverThread = new SstReceiverThread(receiver, n, byteLength);
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
            SstSenderOutput senderOutput = senderThread.getSenderOutput();
            SstReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(pi, senderOutput, receiverOutput);
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
            for (int num = 3; num <= DEFAULT_NUM; num++) {
                SstSender sender = SstFactory.createSender(firstRpc, secondRpc.ownParty(), config);
                SstReceiver receiver = SstFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
                LOGGER.info("-----test {} (precompute) start, (num = {}) -----",
                    sender.getPtoDesc().getPtoName(), num
                );
                int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
                sender.setTaskId(randomTaskId);
                receiver.setTaskId(randomTaskId);
                int[] pi = PermutationNetworkUtils.randomPermutation(num, SECURE_RANDOM);
                byte[] delta = BlockUtils.randomBlock(SECURE_RANDOM);
                CotSenderOutput preSenderOutput = CotSenderOutput.createRandom(
                    SstFactory.getPrecomputeNum(config, num), delta, SECURE_RANDOM
                );
                CotReceiverOutput preReceiverOutput = CotReceiverOutput.createRandom(preSenderOutput, SECURE_RANDOM);
                SstSenderThread senderThread = new SstSenderThread(sender, pi, DEFAULT_BYTE_LENGTH, preReceiverOutput);
                SstReceiverThread receiverThread = new SstReceiverThread(receiver, num, DEFAULT_BYTE_LENGTH, preSenderOutput);
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
                SstSenderOutput senderOutput = senderThread.getSenderOutput();
                SstReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
                assertOutput(pi, senderOutput, receiverOutput);
                printAndResetRpc(time);
                LOGGER.info("-----test {} (precompute) end, (num = {}) -----",
                    sender.getPtoDesc().getPtoName(), num
                );
                // destroy
                new Thread(sender::destroy).start();
                new Thread(receiver::destroy).start();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int[] pi, SstSenderOutput senderOutput, SstReceiverOutput receiverOutput) {
        int num = pi.length;
        Assert.assertArrayEquals(pi, senderOutput.getPi());
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        // Δ = π(a) ⊕ b
        byte[][] a = receiverOutput.getAs();
        byte[][] b = receiverOutput.getBs();
        byte[][] expectDeltas = senderOutput.getDeltas();
        byte[][] pa = PermutationNetworkUtils.permutation(pi, a);
        byte[][] actualDeltas = IntStream.range(0, num)
            .mapToObj(i -> BytesUtils.xor(pa[i], b[i]))
            .toArray(byte[][]::new);
        IntStream.range(0, num).forEach(i -> Assert.assertArrayEquals(expectDeltas[i], actualDeltas[i]));
    }
}
