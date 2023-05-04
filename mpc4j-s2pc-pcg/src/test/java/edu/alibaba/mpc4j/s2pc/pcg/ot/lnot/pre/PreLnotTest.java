package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95.Bea95PreLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.PreLnotFactory.PreLnotType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
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
 * pre-compute 1-out-of-n (with n = 2^l) OT test.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
@RunWith(Parameterized.class)
public class PreLnotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreLnotTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * the default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * default l
     */
    private static final int DEFAULT_L = 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Bea95
        configurations.add(new Object[] {PreLnotType.Bea95.name(), new Bea95PreLnotConfig.Builder().build(),});

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
     * the config
     */
    private final PreLnotConfig config;

    public PreLnotTest(String name, PreLnotConfig config) {
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
    public void test1Num() {
        testPto(1, DEFAULT_L, false);
    }

    @Test
    public void test2Num() {
        testPto(2, DEFAULT_L, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_L, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_L, true);
    }

    @Test
    public void testSmallL() {
        testPto(DEFAULT_NUM, 1, false);
    }

    @Test
    public void testLargeL() {
        testPto(DEFAULT_NUM, 10, false);
    }

    @Test
    public void testLargeNum() {
        testPto(1 << 18, DEFAULT_L, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(1 << 18, DEFAULT_L, true);
    }

    private void testPto(int num, int l, boolean parallel) {
        int n = (1 << l);
        PreLnotSender sender = PreLnotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        PreLnotReceiver receiver = PreLnotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            // pre-compute sender / receiver output
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            LnotSenderOutput preSenderOutput = LnotTestUtils.genSenderOutput(l, num, SECURE_RANDOM);
            LnotReceiverOutput preReceiverOutput = LnotTestUtils.genReceiverOutput(preSenderOutput, SECURE_RANDOM);
            // receiver actual choices
            int[] choiceArray = IntStream.range(0, num)
                .map(index -> SECURE_RANDOM.nextInt(n))
                .toArray();
            PreLnotSenderThread senderThread = new PreLnotSenderThread(sender, preSenderOutput);
            PreLnotReceiverThread receiverThread = new PreLnotReceiverThread(receiver, preReceiverOutput, choiceArray);
            StopWatch stopWatch = new StopWatch();
            // execute the protocol
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
            LnotSenderOutput senderOutput = senderThread.getSenderOutput();
            LnotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // verify
            LnotTestUtils.assertOutput(l, num, senderOutput, receiverOutput);
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
}
