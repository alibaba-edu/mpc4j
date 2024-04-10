package edu.alibaba.mpc4j.s2pc.opf.osn;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.ms13.Ms13OsnConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
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
public class OsnTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OsnTest.class);
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

        // GMR21
        configurations.add(new Object[]{
            OsnFactory.OsnType.GMR21.name(), new Gmr21OsnConfig.Builder(true).build(),
        });
        // MS13
        configurations.add(new Object[]{
            OsnFactory.OsnType.MS13.name(), new Ms13OsnConfig.Builder(true).build(),
        });

        return configurations;
    }

    /**
     * 协议类型
     */
    private final OsnConfig config;

    public OsnTest(String name, OsnConfig config) {
        super(name);
        this.config = config;
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
        OsnSender sender = OsnFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        OsnReceiver receiver = OsnFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
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
            OsnPartyOutput senderOutput = senderThread.getSenderOutput();
            OsnPartyOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(inputVector, permutationMap, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
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
        Vector<ByteBuffer> expectOutputs = PermutationNetworkUtils.permutation(permutationMap, inputVector).stream()
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
