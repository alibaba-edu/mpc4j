package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.ZlEdaBitGenFactory.ZlEdaBitGenType;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.egk20.Egk20ZlEdaBitGenConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Zl edaBit generation test.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
@RunWith(Parameterized.class)
public class ZlEdaBitGenTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlEdaBitGenTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 99;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 14) + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        Zl[] zls = new Zl[]{
            ZlFactory.createInstance(EnvType.STANDARD, 1),
            ZlFactory.createInstance(EnvType.STANDARD, 3),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L - 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L + 1),
        };

        for (Zl zl : zls) {
            int l = zl.getL();
            // EGK20
            configurations.add(new Object[]{
                ZlEdaBitGenType.EGK20.name() + " (" + SecurityModel.SEMI_HONEST + ", l = " + l + ")",
                new Egk20ZlEdaBitGenConfig.Builder(SecurityModel.SEMI_HONEST, zl, false).build(),
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final ZlEdaBitGenConfig config;

    public ZlEdaBitGenTest(String name, ZlEdaBitGenConfig config) {
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
        ZlEdaBitGenParty sender = ZlEdaBitGenFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlEdaBitGenParty receiver = ZlEdaBitGenFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlEdaBitGenPartyThread senderThread = new ZlEdaBitGenPartyThread(sender, num);
            ZlEdaBitGenPartyThread receiverThread = new ZlEdaBitGenPartyThread(receiver, num);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            SquareZlEdaBitVector senderOutput = senderThread.getOutput();
            SquareZlEdaBitVector receiverOutput = receiverThread.getOutput();
            PlainZlEdaBitVector plainZlEdaBitVector = senderOutput.reveal(receiverOutput);
            assertOutput(num, plainZlEdaBitVector);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(int num, PlainZlEdaBitVector plainEdaBitVector) {
        Assert.assertEquals(num, plainEdaBitVector.getNum());
        for (int index = 0; index < num; index++) {
            Assert.assertEquals(plainEdaBitVector.getZlElement(index), plainEdaBitVector.getZ2Element(index));
        }
    }
}
