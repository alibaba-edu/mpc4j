package edu.alibaba.mpc4j.s2pc.opf.osn;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.ms13.Ms13OsnConfig;
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
        Collection<Object[]> configurations = new ArrayList<>();

        // GMR21 (silent)
        configurations.add(new Object[] {
            OsnFactory.OsnType.GMR21.name() + " (silent)",
            new Gmr21OsnConfig.Builder()
                .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true))
                .build(),
        });
        // GMR21 with direct COT
        configurations.add(new Object[] {
            OsnFactory.OsnType.GMR21.name() + " (direct)",
            new Gmr21OsnConfig.Builder()
                .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false))
                .build(),
        });
        // MS13 with silent COT
        configurations.add(new Object[] {
            OsnFactory.OsnType.MS13.name() + " (silent)",
            new Ms13OsnConfig.Builder()
                .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true))
                .build(),
        });
        // MS13 with direct COT
        configurations.add(new Object[] {
            OsnFactory.OsnType.MS13.name() + " (direct)",
            new Ms13OsnConfig.Builder()
                .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false))
                .build(),
        });

        return configurations;
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
        testPto(DEFAULT_N, STATS_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeByteLength() {
        testPto(DEFAULT_N, LARGE_BYTE_LENGTH, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_N, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_N, DEFAULT_BYTE_LENGTH, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_N, LARGE_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_N, LARGE_BYTE_LENGTH, true);
    }

    private void testPto(int n, int byteLength, boolean parallel) {
        OsnSender sender = OsnFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
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
        sender.destroy();
        receiver.destroy();
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
