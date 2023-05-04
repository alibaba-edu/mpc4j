package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64MtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Before;
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
 * 核zp64三元组生成协议测试。
 *
 * @author Liqiang Peng
 * @date 2022/9/7
 */
@RunWith(Parameterized.class)
public class Zp64CoreMtgTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zp64CoreMtgTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 10000;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 8192 * 8;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RSS19 (l = 19)
        configurations.add(new Object[] {
            Zp64CoreMtgFactory.Zp64CoreMtgType.RSS19.name() + " (l = 19)",
            new Rss19Zp64CoreMtgConfig.Builder(19)
                .build()
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
     * the config
     */
    private final Zp64CoreMtgConfig config;

    public Zp64CoreMtgTest(String name, Zp64CoreMtgConfig config) {
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
    public void testDefault() {
        testPto(config, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(config, DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(config, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(config, LARGE_NUM, true);
    }

    private void testPto(Zp64CoreMtgConfig config, int num, boolean parallel) {
        Zp64CoreMtgParty sender = Zp64CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64CoreMtgParty receiver = Zp64CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Zp64CoreMtgPartyThread senderThread = new Zp64CoreMtgPartyThread(sender, num);
            Zp64CoreMtgPartyThread receiverThread = new Zp64CoreMtgPartyThread(receiver, num);
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
            Zp64Triple senderOutput = senderThread.getOutput();
            Zp64Triple receiverOutput = receiverThread.getOutput();
            // 验证结果
            Zp64MtgTestUtils.assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}KB, Receiver sends {}KB, time = {}ms",
                senderByteLength / 1024.0, receiverByteLength / 1024.0, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }
}