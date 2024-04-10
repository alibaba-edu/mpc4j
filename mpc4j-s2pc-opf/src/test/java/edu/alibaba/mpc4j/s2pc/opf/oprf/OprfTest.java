package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory.OprfType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05.Fipr05MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16.Kkrt16OptOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16.Kkrt16OriOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * OPRF test.
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
@RunWith(Parameterized.class)
public class OprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OprfTest.class);
    /**
     * the default batch size
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;
    /**
     * the large batch size
     */
    private static final int LARGE_BATCH_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RS21
        configurations.add(new Object[]{
            OprfType.RS21.name() + " (" + SecurityModel.MALICIOUS + ")",
            new Rs21MpOprfConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[]{
            OprfType.RS21.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new Rs21MpOprfConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // CM20
        configurations.add(new Object[]{
            OprfType.CM20.name(), new Cm20MpOprfConfig.Builder().build(),
        });
        // KKRT16_ORI
        configurations.add(new Object[]{
            OprfType.KKRT16_ORI.name(), new Kkrt16OriOprfConfig.Builder().build(),
        });
        // KKRT16_OPT
        configurations.add(new Object[]{
            OprfType.KKRT16_OPT.name(), new Kkrt16OptOprfConfig.Builder().build(),
        });
        // FIPR05
        configurations.add(new Object[]{
            OprfType.FIPR05.name(), new Fipr05MpOprfConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * the config
     */
    private final OprfConfig config;

    public OprfTest(String name, OprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2N() {
        testPto(2, false);
    }

    @Test
    public void test3N() {
        testPto(3, false);
    }

    @Test
    public void test8N() {
        testPto(8, false);
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
        OprfSender sender = OprfFactory.createOprfSender(firstRpc, secondRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, batch_size = {}-----", sender.getPtoDesc().getPtoName(), batchSize);
            byte[][] inputs = IntStream.range(0, batchSize)
                .mapToObj(index -> {
                    byte[] input = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(input);
                    return input;
                })
                .toArray(byte[][]::new);
            OprfSenderThread senderThread = new OprfSenderThread(sender, batchSize);
            OprfReceiverThread receiverThread = new OprfReceiverThread(receiver, inputs);
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
            OprfSenderOutput senderOutput = senderThread.getSenderOutput();
            OprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchSize, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void assertOutput(int n, OprfSenderOutput senderOutput, OprfReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getPrfByteLength(), receiverOutput.getPrfByteLength());
        Assert.assertEquals(n, senderOutput.getBatchSize());
        Assert.assertEquals(n, receiverOutput.getBatchSize());
        IntStream.range(0, n).forEach(index -> {
            byte[] input = receiverOutput.getInput(index);
            byte[] receiverPrf = receiverOutput.getPrf(index);
            byte[] senderPrf = senderOutput.getPrf(index, input);
            Assert.assertArrayEquals(senderPrf, receiverPrf);
        });
        // all PRFs should be distinct
        long distinctCount = IntStream.range(0, n)
            .mapToObj(receiverOutput::getPrf)
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(receiverOutput.getBatchSize(), distinctCount);
    }
}