package edu.alibaba.mpc4j.s2pc.aby.hamming;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.hamming.bcp13.Bcp13ShHammingConfig;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 汉明距离协议测试。
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
@RunWith(Parameterized.class)
public class HammingTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HammingTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认运算数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 较大运算数量
     */
    private static final int LARGE_NUM = 1 << 18;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // BCP13 (semi-honest)
        configurations.add(new Object[]{
            HammingFactory.HammingType.BCP13_SEMI_HONEST.name(), new Bcp13ShHammingConfig.Builder().build()
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
    private final HammingConfig config;

    public HammingTest(String name, HammingConfig config) {
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
    public void test1Num() {
        testPto(1, false);
    }

    @Test
    public void test2Num() {
        testPto(2, false);
    }

    @Test
    public void test8Num() {
        testPto(8, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, false);
    }

    private void testPto(int num, boolean parallel) {
        HammingParty sender = HammingFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        HammingParty receiver = HammingFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        testAllZeroInputPto(sender, receiver, num);
        testAllOneInputPto(sender, receiver, num);
        testRandomInputPto(sender, receiver, num);
        sender.destroy();
        receiver.destroy();
    }

    private void testAllZeroInputPto(HammingParty sender, HammingParty receiver, int num) {
        int byteLength = CommonUtils.getByteLength(num);
        byte[] x0Bytes = new byte[byteLength];
        byte[] x1Bytes = new byte[byteLength];
        testInputPto(sender, receiver, x0Bytes, x1Bytes, num);
    }

    private void testAllOneInputPto(HammingParty sender, HammingParty receiver, int num) {
        int byteLength = CommonUtils.getByteLength(num);
        byte[] x0Bytes = new byte[byteLength];
        Arrays.fill(x0Bytes, (byte)0xFF);
        BytesUtils.reduceByteArray(x0Bytes, num);
        byte[] x1Bytes = new byte[byteLength];
        Arrays.fill(x1Bytes, (byte)0xFF);
        BytesUtils.reduceByteArray(x1Bytes, num);
        testInputPto(sender, receiver, x0Bytes, x1Bytes, num);
    }

    private void testRandomInputPto(HammingParty sender, HammingParty receiver, int num) {
        int byteLength = CommonUtils.getByteLength(num);
        byte[] x0Bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(x0Bytes);
        BytesUtils.reduceByteArray(x0Bytes, num);
        byte[] x1Bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(x1Bytes);
        BytesUtils.reduceByteArray(x1Bytes, num);
        testInputPto(sender, receiver, x0Bytes, x1Bytes, num);
    }

    private void testInputPto(HammingParty sender, HammingParty receiver, byte[] x0Bytes, byte[] x1Bytes, int num) {
        int expectHammingDistance = BytesUtils.hammingDistance(x0Bytes, x1Bytes);
        assert BytesUtils.isReduceByteArray(x0Bytes, num);
        assert BytesUtils.isReduceByteArray(x1Bytes, num);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        SquareZ2Vector x0 = SquareZ2Vector.create(num, x0Bytes, false);
        SquareZ2Vector x1 = SquareZ2Vector.create(num, x1Bytes, false);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            HammingSenderThread senderThread = new HammingSenderThread(sender, x0);
            HammingReceiverThread receiverThread = new HammingReceiverThread(receiver, x1);
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
            long senderByteLength = senderRpc.getSendByteLength();
            long receiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            int senderHammingDistance = senderThread.getHammingDistance();
            int receiverHammingDistance = receiverThread.getHammingDistance();
            // 验证结果
            assertOutput(expectHammingDistance, senderHammingDistance, receiverHammingDistance);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int expectHammingDistance, int senderHammingDistance, int receiverHammingDistance) {
        Assert.assertEquals(expectHammingDistance, senderHammingDistance);
        Assert.assertEquals(expectHammingDistance, receiverHammingDistance);
    }
}
