package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory.Z2MtgType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.file.FileZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 布尔三元组生成协议测试。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
@RunWith(Parameterized.class)
public class Z2MtgTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2MtgTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 18;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // FILE (Ideal)
        configurations.add(new Object[]{
            Z2MtgType.FILE.name() + " (" + SecurityModel.IDEAL + ")",
            new FileZ2MtgConfig.Builder(SecurityModel.IDEAL).build(),
        });
        // FILE (Semi-honest)
        configurations.add(new Object[]{
            Z2MtgType.FILE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new FileZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // OFFLINE (Ideal)
        configurations.add(new Object[]{
            Z2MtgType.OFFLINE.name() + " (" + SecurityModel.IDEAL + ")",
            new OfflineZ2MtgConfig.Builder(SecurityModel.IDEAL).build(),
        });
        // OFFLINE (Semi-honest)
        configurations.add(new Object[]{
            Z2MtgType.OFFLINE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new OfflineZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // CACHE (Ideal)
        configurations.add(new Object[]{
            Z2MtgType.CACHE.name() + " (" + SecurityModel.IDEAL + ")",
            new OfflineZ2MtgConfig.Builder(SecurityModel.IDEAL).build(),
        });
        // CACHE (Semi-honest)
        configurations.add(new Object[]{
            Z2MtgType.CACHE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new OfflineZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build(),
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
    private final Z2MtgConfig config;

    public Z2MtgTest(String name, Z2MtgConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        Z2MtgParty sender = Z2MtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        Z2MtgParty sender = Z2MtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        Z2MtgParty sender = Z2MtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void testDefault() {
        Z2MtgParty sender = Z2MtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefault() {
        Z2MtgParty sender = Z2MtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        Z2MtgParty sender = Z2MtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        Z2MtgParty sender = Z2MtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM);
    }

    private void testPto(Z2MtgParty sender, Z2MtgParty receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Z2MtgPartyThread senderThread = new Z2MtgPartyThread(sender, num);
            Z2MtgPartyThread receiverThread = new Z2MtgPartyThread(receiver, num);
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
            Z2Triple senderOutput = senderThread.getOutput();
            Z2Triple receiverOutput = receiverThread.getOutput();
            // 验证结果
            Z2MtgTestUtils.assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
