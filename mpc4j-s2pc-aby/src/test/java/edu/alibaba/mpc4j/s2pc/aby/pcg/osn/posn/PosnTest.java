package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.OsnTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.PosnFactory.PosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24.Lll24PosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Pre-computed OSN tests.
 *
 * @author Feng Han
 * @date 2024/5/8
 */
@RunWith(Parameterized.class)
public class PosnTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PosnTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 16) + 1;
    /**
     * σ byte length
     */
    private static final int STATS_BYTE_LENGTH = CommonConstants.STATS_BYTE_LENGTH;
    /**
     * default byte length
     */
    private static final int DEFAULT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * large byte length
     */
    private static final int LARGE_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH * 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // LLL24
        configurations.add(new Object[]{
            PosnType.LLL24.name(), new Lll24PosnConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PosnConfig config;

    public PosnTest(String name, PosnConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2N() {
        testPto(2, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test3N() {
        testPto(3, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test4N() {
        testPto(4, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test5N() {
        testPto(5, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testStatsByteLength() {
        testPto(DEFAULT_NUM, STATS_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeByteLength() {
        testPto(DEFAULT_NUM, LARGE_BYTE_LENGTH, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_NUM, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, DEFAULT_BYTE_LENGTH, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_NUM, LARGE_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_NUM, LARGE_BYTE_LENGTH, true);
    }

    private void testPto(int num, int byteLength, boolean parallel) {
        PosnSender sender = PosnFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PosnReceiver receiver = PosnFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, num = {}, byte_length = {}-----", sender.getPtoDesc().getPtoName(), num, byteLength);
            // generate Random OSN output
            RosnReceiverOutput rosnReceiverOutput = RosnReceiverOutput.createRandom(num, byteLength, SECURE_RANDOM);
            RosnSenderOutput rosnSenderOutput = RosnSenderOutput.createRandom(rosnReceiverOutput, SECURE_RANDOM);
            // generate desired output
            byte[][] inputVector = BytesUtils.randomByteArrayVector(num, byteLength, SECURE_RANDOM);
            int[] pi = PermutationNetworkUtils.randomPermutation(num, SECURE_RANDOM);
            // create thread
            PosnSenderThread senderThread = new PosnSenderThread(sender, inputVector, rosnSenderOutput);
            PosnReceiverThread receiverThread = new PosnReceiverThread(receiver, pi, byteLength, rosnReceiverOutput);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            DosnPartyOutput senderOutput = senderThread.getSenderOutput();
            DosnPartyOutput receiverOutput = receiverThread.getReceiverOutput();
            OsnTestUtils.assertOutput(inputVector, pi, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
