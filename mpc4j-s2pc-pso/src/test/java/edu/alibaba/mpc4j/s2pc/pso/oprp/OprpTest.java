package edu.alibaba.mpc4j.s2pc.pso.oprp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache.CacheZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.file.FileZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc.LowMcOprpConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * OPRP协议测试。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
@RunWith(Parameterized.class)
public class OprpTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OprpTest.class);
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
        // LowMc (file)
        Z2MtgConfig fileZ2MtgConfig = new FileZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build();
        BcConfig fileBcConfig = new Bea91BcConfig.Builder().setZ2MtgConfig(fileZ2MtgConfig).build();
        configurationParams.add(new Object[] {
            OprpFactory.OprpType.LOW_MC.name() + " (file)",
            new LowMcOprpConfig.Builder().setBcConfig(fileBcConfig).build(),
        });
        // LowMc (ideal)
        Z2MtgConfig idealZ2MtgConfig = new CacheZ2MtgConfig.Builder(SecurityModel.IDEAL).build();
        BcConfig idealBcConfig = new Bea91BcConfig.Builder().setZ2MtgConfig(idealZ2MtgConfig).build();
        configurationParams.add(new Object[] {
            OprpFactory.OprpType.LOW_MC.name() + " (ideal)",
            new LowMcOprpConfig.Builder().setBcConfig(idealBcConfig).build(),
        });
        // LowMc (default)
        Z2MtgConfig defaultZ2MtgConfig = new CacheZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build();
        BcConfig defaultBcConfig = new Bea91BcConfig.Builder().setZ2MtgConfig(defaultZ2MtgConfig).build();
        configurationParams.add(new Object[] {
            OprpFactory.OprpType.LOW_MC.name() + " (default)",
            new LowMcOprpConfig.Builder().setBcConfig(defaultBcConfig).build(),
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
    private final OprpConfig config;

    public OprpTest(String name, OprpConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        OprpSender sender = OprpFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OprpReceiver receiver = OprpFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1N() {
        OprpSender sender = OprpFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OprpReceiver receiver = OprpFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2N() {
        OprpSender sender = OprpFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OprpReceiver receiver = OprpFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void testDefault() {
        OprpSender sender = OprpFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OprpReceiver receiver = OprpFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_BATCH_SIZE);
    }

    @Test
    public void testParallelDefault() {
        OprpSender sender = OprpFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OprpReceiver receiver = OprpFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_BATCH_SIZE);
    }

    @Test
    public void testLargeN() {
        OprpSender sender = OprpFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OprpReceiver receiver = OprpFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_BATCH_SIZE);
    }

    @Test
    public void testParallelLargeN() {
        OprpSender sender = OprpFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OprpReceiver receiver = OprpFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_BATCH_SIZE);
    }

    private void testPto(OprpSender sender, OprpReceiver receiver, int batchSize) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
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
            OprpSenderOutput senderOutput = senderThread.getSenderOutput();
            OprpReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(batchSize, key, messages, senderOutput, receiverOutput);
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

    private void assertOutput(int batchSize, byte[] key, byte[][] messages,
        OprpSenderOutput senderOutput, OprpReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getPrpType(), receiverOutput.getPrpType());
        Assert.assertEquals(senderOutput.isInvPrp(), receiverOutput.isInvPrp());
        Assert.assertEquals(batchSize, senderOutput.getN());
        Assert.assertEquals(batchSize, receiverOutput.getN());

        // 明文PRP
        Prp prp = PrpFactory.createInstance(senderOutput.getPrpType());
        prp.setKey(key);
        boolean invPrp = senderOutput.isInvPrp();
        byte[][] ciphertexts = Arrays.stream(messages)
            .map(message -> invPrp ? prp.invPrp(message) : prp.prp(message))
            .toArray(byte[][]::new);
        // 对比密文PRP
        IntStream.range(0, batchSize).forEach(index -> {
            byte[] share = senderOutput.getShare(index);
            BytesUtils.xori(share, receiverOutput.getShare(index));
            Assert.assertArrayEquals(ciphertexts[index], share);
        });
    }
}
