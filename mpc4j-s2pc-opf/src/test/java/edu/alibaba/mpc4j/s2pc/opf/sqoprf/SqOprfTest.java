package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory.SqOprfType;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04.Nr04EccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09.Pssw09SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17ByteEccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17EccSqOprfConfig;
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
 * single-query OPRF test.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
@RunWith(Parameterized.class)
public class SqOprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqOprfTest.class);
    /**
     * the default batch size
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;
    /**
     * the large batch size
     */
    private static final int LARGE_BATCH_SIZE = 1 << 12;
    /**
     * a strange element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = 17;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PSSW09
        configurations.add(new Object[]{
            SqOprfType.PSSW09.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new Pssw09SqOprfConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // NR04_ECC (uncompress)
        configurations.add(new Object[]{
            SqOprfType.NR04_ECC.name() + " (uncompress)",
            new Nr04EccSqOprfConfig.Builder().build(),
        });
        // NR04_ECC (compress)
        configurations.add(new Object[]{
            SqOprfType.NR04_ECC.name() + " (compress)",
            new Nr04EccSqOprfConfig.Builder().setCompressEncode(true).build(),
        });
        // RA17_BYTE_ECC (compress)
        configurations.add(new Object[]{
            SqOprfType.RA17_BYTE_ECC.name(),
            new Ra17ByteEccSqOprfConfig.Builder().build(),
        });
        // RA17_ECC (compress)
        configurations.add(new Object[]{
            SqOprfType.RA17_ECC.name() + " (compress)",
            new Ra17EccSqOprfConfig.Builder().setCompressEncode(true).build(),
        });
        // RA17_ECC (uncompress)
        configurations.add(new Object[]{
            SqOprfType.RA17_ECC.name() + " (uncompress)",
            new Ra17EccSqOprfConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final SqOprfConfig config;

    public SqOprfTest(String name, SqOprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(1, false);
    }

    @Test
    public void test2Num() {
        testPto(2, false);
    }

    @Test
    public void test3Num() {
        testPto(3, false);
    }

    @Test
    public void test8Num() {
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
    public void testLargeNum() {
        testPto(LARGE_BATCH_SIZE, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_BATCH_SIZE, true);
    }

    private void testPto(int batchSize, boolean parallel) {
        SqOprfSender sender = SqOprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SqOprfReceiver receiver = SqOprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, batch_size = {}-----", sender.getPtoDesc().getPtoName(), batchSize);
            byte[][] inputs = IntStream.range(0, batchSize)
                .mapToObj(index -> {
                    byte[] input = new byte[ELEMENT_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(input);
                    return input;
                })
                .toArray(byte[][]::new);
            SqOprfSenderThread senderThread = new SqOprfSenderThread(sender, batchSize);
            SqOprfReceiverThread receiverThread = new SqOprfReceiverThread(receiver, inputs);
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
            SqOprfKey key = senderThread.getKey();
            SqOprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchSize, key, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int batchSize, SqOprfKey key, SqOprfReceiverOutput receiverOutput) {
        Assert.assertEquals(batchSize, receiverOutput.getBatchSize());
        Assert.assertEquals(key.getPrfByteLength(), receiverOutput.getPrfByteLength());
        int prfByteLength = key.getPrfByteLength();
        IntStream.range(0, batchSize).forEach(index -> {
            byte[] input = receiverOutput.getInput(index);
            ByteBuffer receiverPrf = ByteBuffer.wrap(receiverOutput.getPrf(index));
            Assert.assertEquals(prfByteLength, receiverPrf.array().length);

            ByteBuffer senderPrf = ByteBuffer.wrap(key.getPrf(input));

            Assert.assertEquals(prfByteLength, senderPrf.array().length);
            Assert.assertEquals(senderPrf, receiverPrf);
        });
        // all results should be distinct
        long distinctCount = IntStream.range(0, batchSize)
            .mapToObj(receiverOutput::getPrf)
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(receiverOutput.getBatchSize(), distinctCount);
    }


}