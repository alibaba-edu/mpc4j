package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory.Z2MtgType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Z2 multiplication triple generation test.
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
@RunWith(Parameterized.class)
public class Z2MtgTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2MtgTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 999;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 18) + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // OFFLINE (Semi-honest)
        configurations.add(new Object[]{
            Z2MtgType.OFFLINE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new OfflineZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // CACHE (Semi-honest)
        configurations.add(new Object[]{
            Z2MtgType.CACHE.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new OfflineZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final Z2MtgConfig config;

    public Z2MtgTest(String name, Z2MtgConfig config) {
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
        Z2MtgParty sender = Z2MtgFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Z2MtgPartyThread senderThread = new Z2MtgPartyThread(sender, num);
            Z2MtgPartyThread receiverThread = new Z2MtgPartyThread(receiver, num);
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

    @Test
    public void testLessUpdate() {
        int num = DEFAULT_NUM;
        Z2MtgParty sender = Z2MtgFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Z2MtgPartyThread senderThread = new Z2MtgPartyThread(sender, num, num / 2 - 1);
            Z2MtgPartyThread receiverThread = new Z2MtgPartyThread(receiver, num, num / 2 - 1);
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
