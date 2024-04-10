package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15HeZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15OtZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory.ZlCoreMtgType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ZlCoreMtgTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlCoreMtgTest.class);
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

        Zl[] zls = new Zl[] {
            ZlFactory.createInstance(EnvType.STANDARD, 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L - 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L + 1),
            ZlFactory.createInstance(EnvType.STANDARD, CommonConstants.BLOCK_BIT_LENGTH),
        };
        for (Zl zl : zls) {
            int l = zl.getL();
            // DSZ15_HE
            configurations.add(new Object[]{
                ZlCoreMtgType.DSZ15_HE.name() + " (l = " + l + ")", new Dsz15HeZlCoreMtgConfig.Builder(zl).build(),
            });
            // DSZ15_OT
            configurations.add(new Object[]{
                ZlCoreMtgType.DSZ15_OT.name() + " (l = " + l + ")", new Dsz15OtZlCoreMtgConfig.Builder(zl).build(),
            });
        }

        return configurations;
    }

    /**
     * 协议类型
     */
    private final ZlCoreMtgConfig config;

    public ZlCoreMtgTest(String name, ZlCoreMtgConfig config) {
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
        ZlCoreMtgParty sender = ZlCoreMtgFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlCoreMtgParty receiver = ZlCoreMtgFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlCoreMtgPartyThread senderThread = new ZlCoreMtgPartyThread(sender, num);
            ZlCoreMtgPartyThread receiverThread = new ZlCoreMtgPartyThread(receiver, num);
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
            ZlTriple senderOutput = senderThread.getOutput();
            ZlTriple receiverOutput = receiverThread.getOutput();
            ZlMtgTestUtils.assertOutput(config.getZl(), num, senderOutput, receiverOutput);
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
