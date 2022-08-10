package edu.alibaba.mpc4j.s2pc.pcg.ot.no;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.kk13.Kk13OptLhotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.oos17.Oos17LhotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.lh2n.Lh2nNotConfig;
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
 * n选1-OT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
@RunWith(Parameterized.class)
public class NotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotTest.class);
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
     * 较小最大选择值
     */
    private static final int SMALL_N = 2;
    /**
     * 默认最大选择值
     */
    private static final int DEFAULT_N = 5;
    /**
     * 较大最大选择值
     */
    private static final int LARGE_N = 257;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // LH2N (OOS17)
        configurations.add(new Object[] {
            NotFactory.NotType.LH2N.name() + " (" + LotFactory.LotType.OOS17 + ")",
            new Lh2nNotConfig.Builder().setLhotConfig(new Oos17LhotConfig.Builder().build()).build(),
        });
        // LH2N (KK13_OPT)
        configurations.add(new Object[] {
            NotFactory.NotType.LH2N.name() + " (" + LotFactory.LotType.KK13_OPT + ")",
            new Lh2nNotConfig.Builder().setLhotConfig(new Kk13OptLhotConfig.Builder().build()).build(),
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
    private final NotConfig config;

    public NotTest(String name, NotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        NotSender sender = NotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NotReceiver receiver = NotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        NotSender sender = NotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NotReceiver receiver = NotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_N, 1);
    }

    @Test
    public void test2Num() {
        NotSender sender = NotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NotReceiver receiver = NotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_N, 2);
    }

    @Test
    public void testDefault() {
        NotSender sender = NotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NotReceiver receiver = NotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_N, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefault() {
        NotSender sender = NotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NotReceiver receiver = NotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_N, DEFAULT_NUM);
    }

    @Test
    public void testSmallN() {
        NotSender sender = NotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NotReceiver receiver = NotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, SMALL_N, DEFAULT_NUM);
    }

    @Test
    public void testLargeN() {
        NotSender sender = NotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NotReceiver receiver = NotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_N, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        NotSender sender = NotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NotReceiver receiver = NotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_N, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        NotSender sender = NotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NotReceiver receiver = NotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_N, LARGE_NUM);
    }

    private void testPto(NotSender sender, NotReceiver receiver, int n, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int[] choices = IntStream.range(0, num).map(index -> SECURE_RANDOM.nextInt(n)).toArray();
            NotSenderThread senderThread = new NotSenderThread(sender, n, num);
            NotReceiverThread receiverThread = new NotReceiverThread(receiver, n, choices);
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
            NotSenderOutput senderOutput = senderThread.getSenderOutput();
            NotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            assertOutput(n, num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int n, int num, NotSenderOutput senderOutput, NotReceiverOutput receiverOutput) {
        // 验证n
        Assert.assertEquals(n, senderOutput.getN());
        Assert.assertEquals(n, receiverOutput.getN());
        // 验证数量
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        // 验证输出长度
        Assert.assertEquals(senderOutput.getOutputBitLength(), receiverOutput.getOutputBitLength());
        Assert.assertEquals(senderOutput.getOutputByteLength(), receiverOutput.getOutputByteLength());
        IntStream.range(0, num).forEach(index -> {
            int choice = receiverOutput.getChoice(index);
            Assert.assertArrayEquals(receiverOutput.getRb(index), senderOutput.getRb(index, choice));
        });
    }
}
