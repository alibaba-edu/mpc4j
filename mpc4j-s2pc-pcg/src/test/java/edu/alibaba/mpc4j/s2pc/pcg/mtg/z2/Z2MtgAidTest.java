package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pcg.aid.AiderThread;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealAider;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory.Z2MtgType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Z2 multiplication triple generation aid test.
 *
 * @author Weiran Liu
 * @date 2023/5/20
 */
@RunWith(Parameterized.class)
public class Z2MtgAidTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2MtgAidTest.class);
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

        // OFFLINE
        configurations.add(new Object[]{
            Z2MtgType.OFFLINE.name() + " (" + SecurityModel.TRUSTED_DEALER + ")",
            new OfflineZ2MtgConfig.Builder(SecurityModel.TRUSTED_DEALER).build(),
        });
        // CACHE
        configurations.add(new Object[]{
            Z2MtgType.CACHE.name() + " (" + SecurityModel.TRUSTED_DEALER + ")",
            new OfflineZ2MtgConfig.Builder(SecurityModel.TRUSTED_DEALER).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final Z2MtgConfig config;

    public Z2MtgAidTest(String name, Z2MtgConfig config) {
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
        Z2MtgParty sender = Z2MtgFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        Z2MtgParty receiver = Z2MtgFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
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
            Z2MtgPartyThread senderThread = new Z2MtgPartyThread(sender, num);
            Z2MtgPartyThread receiverThread = new Z2MtgPartyThread(receiver, num);
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
            Z2Triple senderOutput = senderThread.getOutput();
            Z2Triple receiverOutput = receiverThread.getOutput();
            Z2MtgTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
