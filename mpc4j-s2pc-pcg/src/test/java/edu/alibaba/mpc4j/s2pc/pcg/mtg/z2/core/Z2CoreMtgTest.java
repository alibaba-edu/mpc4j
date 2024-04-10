package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13.Alsz13Z2CoreMtgConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class Z2CoreMtgTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2CoreMtgTest.class);
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

        // ALSZ13
        configurations.add(new Object[]{
            Z2CoreMtgFactory.Z2CoreMtgType.ALSZ13.name(), new Alsz13Z2CoreMtgConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * 协议类型
     */
    private final Z2CoreMtgConfig config;

    public Z2CoreMtgTest(String name, Z2CoreMtgConfig config) {
        super(name);
        this.config = config;
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
    public void testDefault() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        Z2CoreMtgParty sender = Z2CoreMtgFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Z2CoreMtgParty receiver = Z2CoreMtgFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Z2CoreMtgPartyThread senderThread = new Z2CoreMtgPartyThread(sender, num);
            Z2CoreMtgPartyThread receiverThread = new Z2CoreMtgPartyThread(receiver, num);
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
            Z2Triple senderOutput = senderThread.getOutput();
            Z2Triple receiverOutput = receiverThread.getOutput();
            Z2MtgTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
