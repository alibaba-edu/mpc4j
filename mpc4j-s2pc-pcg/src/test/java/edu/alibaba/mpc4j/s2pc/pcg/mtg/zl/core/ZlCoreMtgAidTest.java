package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.aid.AiderThread;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealAider;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.aid.AidZlCoreMtgConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Zl core multiplication triple generation aid test.
 *
 * @author Weiran Liu
 * @date 2023/6/14
 */
@RunWith(Parameterized.class)
public class ZlCoreMtgAidTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlCoreMtgAidTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;

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
            // AID
            configurations.add(new Object[]{
                ZlCoreMtgFactory.ZlCoreMtgType.AID.name() + " (l = " + l + ")", new AidZlCoreMtgConfig.Builder(zl).build(),
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final ZlCoreMtgConfig config;
    /**
     * Zl instance
     */
    private final Zl zl;

    public ZlCoreMtgAidTest(String name, ZlCoreMtgConfig config) {
        super(name);
        this.config = config;
        zl = config.getZl();
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
    public void testLargeNum() {
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        ZlCoreMtgParty sender = ZlCoreMtgFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        ZlCoreMtgParty receiver = ZlCoreMtgFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
        TrustDealAider aider = new TrustDealAider(thirdRpc, firstRpc.ownParty(), secondRpc.ownParty());
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        aider.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        aider.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlCoreMtgPartyThread senderThread = new ZlCoreMtgPartyThread(sender, num);
            ZlCoreMtgPartyThread receiverThread = new ZlCoreMtgPartyThread(receiver, num);
            AiderThread aiderThread = new AiderThread(aider);
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            aiderThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            ZlTriple senderOutput = senderThread.getOutput();
            ZlTriple receiverOutput = receiverThread.getOutput();
            ZlMtgTestUtils.assertOutput(zl, num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            aiderThread.join();
            new Thread(aider::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
