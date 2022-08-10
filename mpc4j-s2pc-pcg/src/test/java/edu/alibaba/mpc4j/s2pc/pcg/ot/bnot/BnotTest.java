package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15.Co15BnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19BnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99.Np99BnotConfig;
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
 * 基础N选1-OT协议测试。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
@RunWith(Parameterized.class)
public class BnotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BnotTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 256;
    /**
     * 默认选择范围n
     */
    private static final int DEFAULT_N = 512;
    /**
     * 较小选择范围n
     */
    private static final int SMALL_N = 8;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // NP99 + 默认Base-2选1-OT配置
        configurationParams.add(new Object[]{
                BnotFactory.BnotType.NP99.name() + "+ Default Base 2-1 OT",
                new Np99BnotConfig.Builder().build(),
        });
        // CO15 + 压缩编码
        configurationParams.add(new Object[]{
                BnotFactory.BnotType.CO15.name() + " (compress)",
                new Co15BnotConfig.Builder().setCompressEncode(true).build(),
        });
        // CO15 + 非压缩编码
        configurationParams.add(new Object[]{
                BnotFactory.BnotType.CO15.name() + " (uncompress)",
                new Co15BnotConfig.Builder().setCompressEncode(false).build(),
        });
        // NP01 + 压缩编码
        configurationParams.add(new Object[]{
                BnotFactory.BnotType.NP01.name() + " (compress)",
                new Np01BnotConfig.Builder().setCompressEncode(true).build(),
        });
        // NP01 + 非压缩编码
        configurationParams.add(new Object[]{
                BnotFactory.BnotType.NP01.name() + " (uncompress)",
                new Np01BnotConfig.Builder().setCompressEncode(false).build(),
        });
        // MR19 + 压缩编码
        configurationParams.add(new Object[]{
                BnotFactory.BnotType.MR19.name() + " (compress)",
                new Mr19BnotConfig.Builder().setCompressEncode(true).build(),
        });
        // MR19 + 非压缩编码
        configurationParams.add(new Object[]{
                BnotFactory.BnotType.MR19.name() + " (uncompress)",
                new Mr19BnotConfig.Builder().setCompressEncode(false).build(),
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
    private final BnotConfig config;

    public BnotTest(String name, BnotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        BnotSender sender = BnotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BnotReceiver receiver = BnotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        BnotSender sender = BnotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BnotReceiver receiver = BnotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1, SMALL_N, false);
    }

    @Test
    public void test2Num() {
        BnotSender sender = BnotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BnotReceiver receiver = BnotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2, SMALL_N, false);
    }

    @Test
    public void test2NumDefaultN() {
        BnotSender sender = BnotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BnotReceiver receiver = BnotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2, DEFAULT_N, false);
    }

    @Test
    public void testDefaultNum() {
        BnotSender sender = BnotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BnotReceiver receiver = BnotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_N, false);
    }

    @Test
    public void testParallelDefaultNum() {
        BnotSender sender = BnotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BnotReceiver receiver = BnotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_N, true);
    }

    private void testPto(BnotSender sender, BnotReceiver receiver, int num, int n, boolean parallel) {
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int[] choices = new int[num];
            IntStream.range(0, num).forEach(index -> choices[index] = SECURE_RANDOM.nextInt(n));
            BnotSenderThread senderThread = new BnotSenderThread(sender, num, n);
            BnotReceiverThread receiverThread = new BnotReceiverThread(receiver, choices, n);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // 等待线程停止
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long onlineTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // 验证结果
            assertOutput(num, senderThread.getSenderOutput(), receiverThread.getReceiverOutput());
            LOGGER.info("Sender sends {}B, Receiver sends {}B, Online time = {}ms",
                    senderRpc.getSendByteLength(), receiverRpc.getSendByteLength(), onlineTime
            );
            senderRpc.reset();
            receiverRpc.reset();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int num, BnotSenderOutput senderOutput, BnotReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        IntStream.range(0, num).forEach(index -> {
            int choice = receiverOutput.getChoice(index);
            Assert.assertArrayEquals(senderOutput.getRb(index, choice), receiverOutput.getRb(index));
        });
    }
}