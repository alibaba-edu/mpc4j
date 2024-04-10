package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64MtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class Zp64CoreMtgTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zp64CoreMtgTest.class);
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
     * the config
     */
    private final Zp64CoreMtgConfig config;

    public Zp64CoreMtgTest(String name, Zp64CoreMtgConfig config) {
        super(name);
        this.config = config;
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
        Zp64CoreMtgParty sender = Zp64CoreMtgFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Zp64CoreMtgParty receiver = Zp64CoreMtgFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Zp64CoreMtgPartyThread senderThread = new Zp64CoreMtgPartyThread(sender, num);
            Zp64CoreMtgPartyThread receiverThread = new Zp64CoreMtgPartyThread(receiver, num);
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            Zp64Triple senderOutput = senderThread.getOutput();
            Zp64Triple receiverOutput = receiverThread.getOutput();
            Zp64MtgTestUtils.assertOutput(num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}