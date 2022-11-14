package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64MtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 核zp64三元组生成协议测试。
 *
 * @author Liqiang Peng
 * @date 2022/9/7
 */
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
    private static final int LARGE_NUM = 8192*8;
    /**
     *
     */
    private static final int MODULUS_BIT_LENGTH = 20;
    /**
     * 发送方
     */
    private final Rpc senderRpc;
    /**
     * 接收方
     */
    private final Rpc receiverRpc;

    public Zp64CoreMtgTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testRandom() {
        System.out.println(SECURE_RANDOM.nextLong());
    }

    @Test
    public void testDefaultNum() {
        try {
            Zp64CoreMtgConfig config = new Rss19Zp64CoreMtgConfig.Builder()
                .setPlainModulusSize(MODULUS_BIT_LENGTH)
                .build();
            LOGGER.info("----- config build succeed -----");
            Zp64CoreMtgParty sender = Zp64CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
            Zp64CoreMtgParty receiver = Zp64CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
            testPto(sender, receiver, DEFAULT_NUM);
        } catch (Exception e) {
            LOGGER.info("----- config build failed : " + e);
        }
    }

    @Test
    public void testLargeNum() {
        try {
            Zp64CoreMtgConfig config = new Rss19Zp64CoreMtgConfig.Builder()
                .setPlainModulusSize(MODULUS_BIT_LENGTH)
                .build();
            LOGGER.info("----- config build succeed -----");
            Zp64CoreMtgParty sender = Zp64CoreMtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
            Zp64CoreMtgParty receiver = Zp64CoreMtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
            testPto(sender, receiver, LARGE_NUM);
        } catch (Exception e) {
            LOGGER.info("----- config build failed : " + e);
        }
    }

    @Test
    public void testConfigSetPlainModulusSize() {
        for (int size = 1; size < Long.SIZE - 1; size++) {
            try {
                Rss19Zp64CoreMtgConfig config = new Rss19Zp64CoreMtgConfig.Builder()
                    .setPlainModulusSize(size)
                    .build();
                long prime = config.getZp();
                long primeBitLength = LongUtils.ceilLog2(prime);
                LOGGER.info("config build success for plain bit length {}, prime = {} ({})", size, prime, primeBitLength);
            } catch (Exception e) {
                LOGGER.info("config build  failed for plain bit length {}: ", size);
            }
        }
    }

    @Test
    public void testConfigSetPolyModulusDegree() {
        int polyModulusDegree = 2048;
        for (int size = 1; size < Long.SIZE - 1; size++) {
            LOGGER.info(String.valueOf(size));
            try {
                Rss19Zp64CoreMtgConfig config = new Rss19Zp64CoreMtgConfig.Builder()
                    .setPolyModulusDegree(polyModulusDegree, size)
                    .build();
                long prime = config.getZp();
                long primeBitLength = LongUtils.ceilLog2(prime);
                LOGGER.info("config build success for modulus degree {}, prime = {} ({})", size, prime, primeBitLength);
            } catch (Exception e) {
                LOGGER.info("config build failed : " + e);
            }
        }
    }

    private void testPto(Zp64CoreMtgParty sender, Zp64CoreMtgParty receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
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
    }
}