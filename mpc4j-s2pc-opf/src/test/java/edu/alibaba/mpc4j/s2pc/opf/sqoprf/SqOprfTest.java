package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory.SqOprfType;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17ByteEccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17EccSqOprfConfig;
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

import java.nio.ByteBuffer;
import java.security.SecureRandom;
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
public class SqOprfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqOprfTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * the default batch size
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;
    /**
     * the large batch size
     */
    private static final int LARGE_BATCH_SIZE = 1 << 14;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

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
    private final SqOprfConfig config;

    public SqOprfTest(String name, SqOprfConfig config) {
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
    public void test1N() {
        testPto(1, false);
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
        SqOprfSender sender = SqOprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        SqOprfReceiver receiver = SqOprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            SqOprfSenderThread senderThread = new SqOprfSenderThread(sender, batchSize);
            SqOprfReceiverThread receiverThread = new SqOprfReceiverThread(receiver, inputs);
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
            SqOprfKey key = senderThread.getKey();
            SqOprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(batchSize, key, receiverOutput);
            LOGGER.info("Sender data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                senderRpc.getSendDataPacketNum(), senderRpc.getPayloadByteLength(), senderRpc.getSendByteLength(),
                time
            );
            LOGGER.info("Receiver data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                receiverRpc.getSendDataPacketNum(), receiverRpc.getPayloadByteLength(), receiverRpc.getSendByteLength(),
                time
            );
            senderRpc.reset();
            receiverRpc.reset();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
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