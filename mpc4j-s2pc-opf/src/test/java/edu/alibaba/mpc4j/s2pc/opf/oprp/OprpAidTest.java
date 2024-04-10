package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcOprpConfig;
import edu.alibaba.mpc4j.s2pc.pcg.aid.AiderThread;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealAider;
import org.apache.commons.lang3.time.StopWatch;
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
 * OPRP aid test.
 *
 * @author Weiran Liu
 * @date 2023/6/26
 */
@RunWith(Parameterized.class)
public class OprpAidTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OprpAidTest.class);
    /**
     * default batch size
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;
    /**
     * large batch size
     */
    private static final int LARGE_BATCH_SIZE = 1 << 14;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // LowMC
        configurations.add(new Object[] {
            OprpFactory.OprpType.LOW_MC.name() + " (" + SecurityModel.TRUSTED_DEALER.name() + ")",
            new LowMcOprpConfig.Builder(SecurityModel.TRUSTED_DEALER)
                .build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final OprpConfig config;

    public OprpAidTest(String name, OprpConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1N() {
        testPto(1, false);
    }

    @Test
    public void test2N() {
        testPto(2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_BATCH_SIZE, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_BATCH_SIZE, true);
    }

    @Test
    public void testLargeN() {
        testPto(LARGE_BATCH_SIZE, false);
    }

    @Test
    public void testParallelLargeN() {
        testPto(LARGE_BATCH_SIZE, true);
    }

    private void testPto(int batchSize, boolean parallel) {
        OprpSender sender = OprpFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        OprpReceiver receiver = OprpFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
        TrustDealAider aider = new TrustDealAider(thirdRpc, firstRpc.ownParty(), secondRpc.ownParty());
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        aider.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        aider.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, batch_size = {}-----", sender.getPtoDesc().getPtoName(), batchSize);
            byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(key);
            byte[][] messages = IntStream.range(0, batchSize)
                .mapToObj(index -> {
                    byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(message);
                    return message;
                })
                .toArray(byte[][]::new);
            OprpSenderThread senderThread = new OprpSenderThread(sender, key, batchSize);
            OprpReceiverThread receiverThread = new OprpReceiverThread(receiver, messages);
            AiderThread aiderThread = new AiderThread(aider);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            aiderThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            OprpSenderOutput senderOutput = senderThread.getSenderOutput();
            OprpReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchSize, key, messages, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            aiderThread.join();
            new Thread(aider::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int batchSize, byte[] key, byte[][] messages,
        OprpSenderOutput senderOutput, OprpReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getPrpType(), receiverOutput.getPrpType());
        Assert.assertEquals(senderOutput.isInvPrp(), receiverOutput.isInvPrp());
        Assert.assertEquals(batchSize, senderOutput.getN());
        Assert.assertEquals(batchSize, receiverOutput.getN());

        // plain PRP
        Prp prp = PrpFactory.createInstance(senderOutput.getPrpType());
        prp.setKey(key);
        boolean invPrp = senderOutput.isInvPrp();
        byte[][] ciphertexts = Arrays.stream(messages)
            .map(message -> invPrp ? prp.invPrp(message) : prp.prp(message))
            .toArray(byte[][]::new);
        // MPC PRP
        IntStream.range(0, batchSize).forEach(index -> {
            byte[] share = senderOutput.getShare(index);
            BytesUtils.xori(share, receiverOutput.getShare(index));
            Assert.assertArrayEquals(ciphertexts[index], share);
        });
    }
}
