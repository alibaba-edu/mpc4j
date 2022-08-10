package edu.alibaba.mpc4j.s2pc.pso.oprf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pso.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.kkrt16.Kkrt16OptOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.kkrt16.Kkrt16OriOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.ra17.Ra17MpOprfConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
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
 * OPRF协议测试。
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
@RunWith(Parameterized.class)
public class OprfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OprfTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认批处理数量
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;
    /**
     * 较大批处理数量
     */
    private static final int LARGE_BATCH_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // CM20
        configurationParams.add(new Object[]{
            OprfFactory.OprfType.CM20.name(),
            new Cm20MpOprfConfig.Builder().build(),
        });
        // RA17，压缩编码
        configurationParams.add(new Object[]{
            OprfFactory.OprfType.RA17.name() + " (compress)",
            new Ra17MpOprfConfig.Builder().setCompressEncode(true).build(),
        });
        // RA17，非压缩编码
        configurationParams.add(new Object[]{
            OprfFactory.OprfType.RA17.name() + " (uncompress)",
            new Ra17MpOprfConfig.Builder().setCompressEncode(false).build(),
        });
        // KKRT16_ORI
        configurationParams.add(new Object[]{
            OprfFactory.OprfType.KKRT16_ORI.name(),
            new Kkrt16OriOprfConfig.Builder().build(),
        });
        // KKRT16_OPT
        configurationParams.add(new Object[]{
            OprfFactory.OprfType.KKRT16_OPT.name(),
            new Kkrt16OptOprfConfig.Builder().build(),
        });

        return configurationParams;
    }

    /**
     * 发送方
     */
    private final Rpc senderRpc;
    /**
     * 接收方
     */
    private final Rpc receiverRpc;
    /**
     * 协议类型
     */
    private final OprfConfig config;

    public OprfTest(String name, OprfConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        OprfSender sender = OprfFactory.createOprfSender(senderRpc, receiverRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test2N() {
        OprfSender sender = OprfFactory.createOprfSender(senderRpc, receiverRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void test3N() {
        OprfSender sender = OprfFactory.createOprfSender(senderRpc, receiverRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 3);
    }

    @Test
    public void test8N() {
        OprfSender sender = OprfFactory.createOprfSender(senderRpc, receiverRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 8);
    }

    @Test
    public void testDefault() {
        OprfSender sender = OprfFactory.createOprfSender(senderRpc, receiverRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_BATCH_SIZE);
    }

    @Test
    public void testParallelDefault() {
        OprfSender sender = OprfFactory.createOprfSender(senderRpc, receiverRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_BATCH_SIZE);
    }

    @Test
    public void testLargeN() {
        OprfSender sender = OprfFactory.createOprfSender(senderRpc, receiverRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_BATCH_SIZE);
    }

    @Test
    public void testParallelLargeN() {
        OprfSender sender = OprfFactory.createOprfSender(senderRpc, receiverRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_BATCH_SIZE);
    }

    private void testPto(OprfSender sender, OprfReceiver receiver, int batchSize) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
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
            // 开始执行协议
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            OprfSenderOutput senderOutput = senderThread.getSenderOutput();
            OprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(batchSize, senderOutput, receiverOutput);
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
    }

    private void assertOutput(int n, OprfSenderOutput senderOutput, OprfReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getPrfByteLength(), receiverOutput.getPrfByteLength());
        Assert.assertEquals(n, senderOutput.getBatchSize());
        Assert.assertEquals(n, receiverOutput.getBatchSize());
        IntStream.range(0, n).forEach(index -> {
            byte[] input = receiverOutput.getInput(index);
            byte[] receiverPrf = receiverOutput.getPrf(index);
            byte[] senderPrf = senderOutput.getPrf(index, input);
            Assert.assertArrayEquals(senderPrf, receiverPrf);
        });
        // 所有结果都应不相同
        long distinctCount = IntStream.range(0, n).mapToObj(receiverOutput::getPrf).map(ByteBuffer::wrap).distinct().count();
        Assert.assertEquals(receiverOutput.getBatchSize(),  distinctCount);
    }
}