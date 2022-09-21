package edu.alibaba.mpc4j.s2pc.pcg.ot.lot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
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
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 2^l选1-OT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
@RunWith(Parameterized.class)
public class LotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LotTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1 << 10;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * 较小输入比特长度
     */
    private static final int SMALL_INPUT_BIT_LENGTH = 1;
    /**
     * 默认输入比特长度
     */
    private static final int DEFAULT_INPUT_BIT_LENGTH = 8;
    /**
     * 较大输入比特长度
     */
    private static final int LARGE_INPUT_BIT_LENGTH = 64;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        return new ArrayList<>();
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
    private final LotConfig config;

    public LotTest(String name, LotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        LotSender sender = LotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LotReceiver receiver = LotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        LotSender sender = LotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LotReceiver receiver = LotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_INPUT_BIT_LENGTH, 1);
    }

    @Test
    public void test2Num() {
        LotSender sender = LotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LotReceiver receiver = LotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_INPUT_BIT_LENGTH, 2);
    }

    @Test
    public void testDefault() {
        LotSender sender = LotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LotReceiver receiver = LotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_INPUT_BIT_LENGTH, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefault() {
        LotSender sender = LotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LotReceiver receiver = LotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_INPUT_BIT_LENGTH, DEFAULT_NUM);
    }

    @Test
    public void testSmallInputBitLength() {
        LotSender sender = LotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LotReceiver receiver = LotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, SMALL_INPUT_BIT_LENGTH, DEFAULT_NUM);
    }

    @Test
    public void testLargeInputBitLength() {
        LotSender sender = LotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LotReceiver receiver = LotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_INPUT_BIT_LENGTH, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        LotSender sender = LotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LotReceiver receiver = LotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_INPUT_BIT_LENGTH, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        LotSender sender = LotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        LotReceiver receiver = LotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_INPUT_BIT_LENGTH, LARGE_NUM);
    }

    private void testPto(LotSender sender, LotReceiver receiver, int inputBitLength, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int inputByteLength = CommonUtils.getByteLength(inputBitLength);
            byte[][] choices = IntStream.range(0, num)
                .mapToObj(index -> {
                    byte[] choice = new byte[inputByteLength];
                    SECURE_RANDOM.nextBytes(choice);
                    BytesUtils.reduceByteArray(choice, inputBitLength);
                    return choice;
                })
                .toArray(byte[][]::new);
            LotSenderThread senderThread = new LotSenderThread(sender, inputBitLength, num);
            LotReceiverThread receiverThread = new LotReceiverThread(receiver, inputBitLength, choices);
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
            LotSenderOutput senderOutput = senderThread.getSenderOutput();
            LotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(inputBitLength, num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int inputBitLength, int num, LotSenderOutput senderOutput, LotReceiverOutput receiverOutput) {
        // 验证输入长度
        Assert.assertEquals(inputBitLength, senderOutput.getInputBitLength());
        Assert.assertEquals(inputBitLength, receiverOutput.getInputBitLength());
        Assert.assertEquals(senderOutput.getInputByteLength(), receiverOutput.getInputByteLength());
        // 验证输出长度
        Assert.assertEquals(senderOutput.getOutputBitLength(), receiverOutput.getOutputBitLength());
        Assert.assertEquals(senderOutput.getOutputByteLength(), receiverOutput.getOutputByteLength());
        // 验证数量
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getRbArray().length);
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            IntStream.range(0, num).forEach(index -> {
                byte[] choice = receiverOutput.getChoice(index);
                byte[] senderRb = senderOutput.getRb(index, choice);
                Assert.assertArrayEquals(receiverOutput.getRb(index), senderRb);
            });
        }

    }
}
