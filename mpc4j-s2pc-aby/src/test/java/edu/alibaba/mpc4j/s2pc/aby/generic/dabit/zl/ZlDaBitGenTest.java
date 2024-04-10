package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.ZlDaBitGenFactory.ZlDaBitGenType;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20.Egk20MacZlDaBitGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20.Egk20NoMacZlDaBitGenConfig;
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
 * Zl daBit generation test.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
@RunWith(Parameterized.class)
public class ZlDaBitGenTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlDaBitGenTest.class);
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
            // EGK20_MAC
            configurations.add(new Object[]{
                ZlDaBitGenType.EGK20_MAC.name() + " (" + SecurityModel.SEMI_HONEST.name() + ", l = " + l + ")",
                new Egk20MacZlDaBitGenConfig.Builder(SecurityModel.SEMI_HONEST, zl, false).build(),
            });
            // EGK20_NO_MAC
            configurations.add(new Object[]{
                ZlDaBitGenType.EGK20_NO_MAC.name() + " (" + SecurityModel.SEMI_HONEST.name() + ", l = " + l + ")",
                new Egk20NoMacZlDaBitGenConfig.Builder(SecurityModel.SEMI_HONEST, zl, false).build(),
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final ZlDaBitGenConfig config;

    public ZlDaBitGenTest(String name, ZlDaBitGenConfig config) {
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
        ZlDaBitGenParty sender = ZlDaBitGenFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlDaBitGenParty receiver = ZlDaBitGenFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlDaBitGenPartyThread senderThread = new ZlDaBitGenPartyThread(sender, num);
            ZlDaBitGenPartyThread receiverThread = new ZlDaBitGenPartyThread(receiver, num);
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
            SquareZlDaBitVector senderOutput = senderThread.getOutput();
            SquareZlDaBitVector receiverOutput = receiverThread.getOutput();
            PlainZlDaBitVector plainZlDaBitVector = senderOutput.reveal(receiverOutput);
            // verify
            assertOutput(num, plainZlDaBitVector);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(int num, PlainZlDaBitVector plainDaBitVector) {
        Assert.assertEquals(num, plainDaBitVector.getNum());
        for (int index = 0; index < num; index++) {
            Assert.assertEquals(plainDaBitVector.getZlElement(index), plainDaBitVector.getZ2Element(index));
        }
    }
}
