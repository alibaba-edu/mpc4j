package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory.LnotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.direct.DirectLnotConfig;
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
 * 1-out-of-n (with n = 2^l) OT test.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
@RunWith(Parameterized.class)
public class LnotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LnotTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default l
     */
    private static final int DEFAULT_L = 5;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * small l
     */
    private static final int SMALL_L = 1;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CACHE (Malicious)
        configurations.add(new Object[]{
            LnotType.CACHE.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectLnotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // CACHE (Semi-honest)
        configurations.add(new Object[]{
            LnotType.CACHE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectLnotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // DIRECT (Malicious)
        configurations.add(new Object[]{
            LnotType.DIRECT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectLnotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // DIRECT (Semi-honest)
        configurations.add(new Object[]{
            LnotType.DIRECT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectLnotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * sender RPC
     */
    private final Rpc senderRpc;
    /**
     * receiver RPC
     */
    private final Rpc receiverRpc;
    /**
     * config
     */
    private final LnotConfig config;

    public LnotTest(String name, LnotConfig config) {
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
    public void test1Round1Num() {
        testPto(DEFAULT_L, 1, false);
    }

    @Test
    public void test2Round2Num() {
        testPto(DEFAULT_L, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_L, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_L, DEFAULT_NUM, true);
    }

    @Test
    public void testSmallL() {
        testPto(SMALL_L, DEFAULT_NUM, false);
    }

    @Test
    public void testLog12Num() {
        testPto(DEFAULT_L, 1 << 12, false);
    }

    @Test
    public void testLog16Num() {
        testPto(DEFAULT_L, 1 << 16, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, true);
    }

    private void testPto(int l, int num, boolean parallel) {
        LnotSender sender = LnotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LnotReceiver receiver = LnotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate the input
        int n = (1 << l);
        int[] choiceArray = IntStream.range(0, num)
            .map(index -> SECURE_RANDOM.nextInt(n))
            .toArray();
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            LnotSenderThread senderThread = new LnotSenderThread(sender, l, num);
            LnotReceiverThread receiverThread = new LnotReceiverThread(receiver, l, choiceArray);
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
            senderRpc.reset();
            long receiverByteLength = receiverRpc.getSendByteLength();
            receiverRpc.reset();
            LnotSenderOutput senderOutput = senderThread.getSenderOutput();
            LnotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // verify
            LnotTestUtils.assertOutput(l, num, senderOutput, receiverOutput);
            int[] actualChoiceArray = IntStream.range(0, num)
                .map(receiverOutput::getChoice)
                .toArray();
            Assert.assertArrayEquals(choiceArray, actualChoiceArray);
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
