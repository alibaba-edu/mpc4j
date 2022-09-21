package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal.IdealZlCoreMtgConfig;
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
 * 核l比特三元组生成协议测试。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
@RunWith(Parameterized.class)
public class ZlCoreMtgTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlCoreMtgTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 100;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // IDEAL (l = 1)
        configurations.add(new Object[]{
            ZlCoreMtgFactory.ZlCoreMtgType.IDEAL.name() + " (l = 1)", new IdealZlCoreMtgConfig.Builder(1).build(),
        });
        // IDEAL (l = 63)
        configurations.add(new Object[]{
            ZlCoreMtgFactory.ZlCoreMtgType.IDEAL.name() + " (l = 9)", new IdealZlCoreMtgConfig.Builder(63).build(),
        });
        // IDEAL (l = 128)
        configurations.add(new Object[]{
            ZlCoreMtgFactory.ZlCoreMtgType.IDEAL.name() + " (l = 32)", new IdealZlCoreMtgConfig.Builder(128).build(),
        });

        // DSZ15 (l = 1)
        configurations.add(new Object[]{
            ZlCoreMtgFactory.ZlCoreMtgType.DSZ15.name() + " (l = 1)", new Dsz15ZlCoreMtgConfig.Builder(1).build(),
        });
        // DSZ15 (l = 63)
        configurations.add(new Object[]{
            ZlCoreMtgFactory.ZlCoreMtgType.DSZ15.name() + " (l = 9)", new Dsz15ZlCoreMtgConfig.Builder(63).build(),
        });
        // DSZ15 (l = 128)
        configurations.add(new Object[]{
            ZlCoreMtgFactory.ZlCoreMtgType.DSZ15.name() + " (l = 32)", new Dsz15ZlCoreMtgConfig.Builder(128).build(),
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
    private final ZlCoreMtgConfig config;

    public ZlCoreMtgTest(String name, ZlCoreMtgConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        ZlCoreMtgParty sender = ZlCoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZlCoreMtgParty receiver = ZlCoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        ZlCoreMtgParty sender = ZlCoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZlCoreMtgParty receiver = ZlCoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        ZlCoreMtgParty sender = ZlCoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZlCoreMtgParty receiver = ZlCoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void testDefault() {
        ZlCoreMtgParty sender = ZlCoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZlCoreMtgParty receiver = ZlCoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefault() {
        ZlCoreMtgParty sender = ZlCoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZlCoreMtgParty receiver = ZlCoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        ZlCoreMtgParty sender = ZlCoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZlCoreMtgParty receiver = ZlCoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        ZlCoreMtgParty sender = ZlCoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZlCoreMtgParty receiver = ZlCoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM);
    }

    private void testPto(ZlCoreMtgParty sender, ZlCoreMtgParty receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlCoreMtgPartyThread senderThread = new ZlCoreMtgPartyThread(sender, num);
            ZlCoreMtgPartyThread receiverThread = new ZlCoreMtgPartyThread(receiver, num);
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
            ZlTriple senderOutput = senderThread.getOutput();
            ZlTriple receiverOutput = receiverThread.getOutput();
            // 验证结果
            ZlMtgTestUtils.assertOutput(config.getL(), num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
