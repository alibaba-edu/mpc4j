package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.DaBitTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.ZlDaBitTuple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.ZlDaBitGenFactory.ZlDaBitGenType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.lkz24.Lkz24ZlDaBitGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.plg24.Plg24ZlDaBitGenConfig;
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

        // PLG24
        configurations.add(new Object[]{
            ZlDaBitGenType.PLG24.name(), new Plg24ZlDaBitGenConfig.Builder(SecurityModel.SEMI_HONEST, false).build(),
        });
        // LZK24
        configurations.add(new Object[]{
            ZlDaBitGenType.LZK24.name(), new Lkz24ZlDaBitGenConfig.Builder(SecurityModel.SEMI_HONEST, false).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final ZlDaBitGenConfig config;
    /**
     * Zl
     */
    private final Zl zl;

    public ZlDaBitGenTest(String name, ZlDaBitGenConfig config) {
        super(name);
        this.config = config;
        zl = ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L_FOR_MODULE_N - 1);
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
        // init Zl circuit parties
        ZlDaBitGenParty sender = ZlDaBitGenFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlDaBitGenParty receiver = ZlDaBitGenFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlDaBitGenPartyThread senderThread = new ZlDaBitGenPartyThread(sender, zl, num);
            ZlDaBitGenPartyThread receiverThread = new ZlDaBitGenPartyThread(receiver, zl, num);
            // start
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            ZlDaBitTuple senderOutput = senderThread.getOutput();
            ZlDaBitTuple receiverOutput = receiverThread.getOutput();
            DaBitTestUtils.assertOutput(zl, num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }
}
