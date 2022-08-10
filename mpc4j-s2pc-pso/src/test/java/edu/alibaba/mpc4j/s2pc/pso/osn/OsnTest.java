package edu.alibaba.mpc4j.s2pc.pso.osn;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.ms13.Ms13OsnConfig;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OSN协议测试。
 *
 * @author Weiran Liu
 * @date 2022/02/10
 */
@RunWith(Parameterized.class)
public class OsnTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OsnTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认批处理数量
     */
    private static final int DEFAULT_N = 1000;
    /**
     * 性能测试置换表大小
     */
    private static final int LARGE_N = 200000;
    /**
     * 统计字节长度
     */
    private static final int STATS_BYTE_LENGTH = CommonConstants.STATS_BYTE_LENGTH;
    /**
     * 默认字节长度
     */
    private static final int DEFAULT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 较大字节长度
     */
    private static final int LARGE_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH * 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // GMR21 (silent COT)
        configurationParams.add(new Object[] {
            OsnFactory.OsnType.GMR21.name() + " (silent COT)",
            new Gmr21OsnConfig.Builder()
                .setCotConfig(new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build(),
        });
        // GMR21 with direct COT
        configurationParams.add(new Object[] {
            OsnFactory.OsnType.GMR21.name() + " (direct COT)",
            new Gmr21OsnConfig.Builder()
                .setCotConfig(new DirectCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build(),
        });
        // MS13 with silent COT
        configurationParams.add(new Object[] {
            OsnFactory.OsnType.MS13.name() + " (silent COT)",
            new Ms13OsnConfig.Builder()
                .setCotConfig(new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build(),
        });
        // MS13 with direct COT
        configurationParams.add(new Object[] {
            OsnFactory.OsnType.MS13.name() + " (direct COT)",
            new Ms13OsnConfig.Builder()
                .setCotConfig(new DirectCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build(),
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
    private final OsnConfig config;

    public OsnTest(String name, OsnConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test2N() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2, DEFAULT_BYTE_LENGTH);
    }

    @Test
    public void test3N() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 3, DEFAULT_BYTE_LENGTH);
    }

    @Test
    public void test4N() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 4, DEFAULT_BYTE_LENGTH);
    }

    @Test
    public void test5N() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 5, DEFAULT_BYTE_LENGTH);
    }

    @Test
    public void testStatsByteLength() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_N, STATS_BYTE_LENGTH);
    }

    @Test
    public void testLargeByteLength() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_N, LARGE_BYTE_LENGTH);
    }

    @Test
    public void testDefault() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_N, DEFAULT_BYTE_LENGTH);
    }

    @Test
    public void testParallelDefault() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_N, DEFAULT_BYTE_LENGTH);
    }

    @Test
    public void testLarge() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_N, LARGE_BYTE_LENGTH);
    }

    @Test
    public void testParallelLarge() {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_N, LARGE_BYTE_LENGTH);
    }

    private void testPto(OsnSender sender, OsnReceiver receiver, int n, int byteLength) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, n = {}, byte_length = {}-----", sender.getPtoDesc().getPtoName(), n, byteLength);
            // 生成输入向量
            Vector<byte[]> inputVector = IntStream.range(0, n)
                .mapToObj(index -> {
                    byte[] input = new byte[byteLength];
                    SECURE_RANDOM.nextBytes(input);
                    return input;
                })
                .collect(Collectors.toCollection(Vector::new));
            // 生成置换映射
            List<Integer> shufflePermutationMap = IntStream.range(0, n).boxed().collect(Collectors.toList());
            Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
            int[] permutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
            OsnSenderThread senderThread = new OsnSenderThread(sender, inputVector, byteLength);
            OsnReceiverThread receiverThread = new OsnReceiverThread(receiver, permutationMap, byteLength);
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
            OsnPartyOutput senderOutput = senderThread.getSenderOutput();
            OsnPartyOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(inputVector, permutationMap, senderOutput, receiverOutput);
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

    private void assertOutput(Vector<byte[]> inputVector, int[] permutationMap,
        OsnPartyOutput senderOutput, OsnPartyOutput receiverOutput) {
        int n = inputVector.size();
        Assert.assertEquals(permutationMap.length, n);
        Assert.assertEquals(senderOutput.getN(), n);
        Assert.assertEquals(receiverOutput.getN(), n);
        Assert.assertEquals(senderOutput.getByteLength(), receiverOutput.getByteLength());
        Vector<ByteBuffer> expectOutputs = BenesNetworkUtils.permutation(permutationMap, inputVector).stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(Vector::new));
        Vector<ByteBuffer> actualOutputs = IntStream.range(0, n)
            .mapToObj(index ->
                BytesUtils.xor(senderOutput.getShare(index), receiverOutput.getShare(index))
            )
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(Vector::new));
        Assert.assertEquals(expectOutputs, actualOutputs);
    }
}
