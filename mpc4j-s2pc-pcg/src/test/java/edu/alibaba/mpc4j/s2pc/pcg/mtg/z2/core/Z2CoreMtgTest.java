package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13.Alsz13Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.ideal.IdealZ2CoreMtgConfig;
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

/**
 * 核布尔三元组生成协议测试。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
@RunWith(Parameterized.class)
public class Z2CoreMtgTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2CoreMtgTest.class);
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

        // IDEAL
        configurations.add(new Object[]{
            Z2CoreMtgFactory.Z2CoreMtgType.IDEAL.name(), new IdealZ2CoreMtgConfig.Builder().build(),
        });
        // ALSZ13
        configurations.add(new Object[]{
            Z2CoreMtgFactory.Z2CoreMtgType.ALSZ13.name(), new Alsz13Z2CoreMtgConfig.Builder().build(),
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
    private final Z2CoreMtgConfig config;

    public Z2CoreMtgTest(String name, Z2CoreMtgConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        Z2CoreMtgParty sender = Z2CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreMtgParty receiver = Z2CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        Z2CoreMtgParty sender = Z2CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreMtgParty receiver = Z2CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        Z2CoreMtgParty sender = Z2CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreMtgParty receiver = Z2CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void testDefault() {
        Z2CoreMtgParty sender = Z2CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreMtgParty receiver = Z2CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefault() {
        Z2CoreMtgParty sender = Z2CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreMtgParty receiver = Z2CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        Z2CoreMtgParty sender = Z2CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreMtgParty receiver = Z2CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        Z2CoreMtgParty sender = Z2CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2CoreMtgParty receiver = Z2CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM);
    }

    private void testPto(Z2CoreMtgParty sender, Z2CoreMtgParty receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Z2CoreMtgPartyThread senderThread = new Z2CoreMtgPartyThread(sender, num);
            Z2CoreMtgPartyThread receiverThread = new Z2CoreMtgPartyThread(receiver, num);
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
