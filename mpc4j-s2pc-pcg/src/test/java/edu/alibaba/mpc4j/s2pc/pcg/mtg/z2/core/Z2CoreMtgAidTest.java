package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pcg.aid.AiderThread;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealAider;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.aid.AidZ2CoreMtgConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Z2 core multiplication triple generation aid test.
 *
 * @author Weiran Liu
 * @date 2023/5/20
 */
public class Z2CoreMtgAidTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2CoreMtgAidTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * config
     */
    private final Z2CoreMtgConfig config;

    public Z2CoreMtgAidTest() {
        super(Z2CoreMtgFactory.Z2CoreMtgType.AID.name());
        this.config = new AidZ2CoreMtgConfig.Builder().build();
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
        Z2CoreMtgParty sender = Z2CoreMtgFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        Z2CoreMtgParty receiver = Z2CoreMtgFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
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
            Z2CoreMtgPartyThread senderThread = new Z2CoreMtgPartyThread(sender, num);
            Z2CoreMtgPartyThread receiverThread = new Z2CoreMtgPartyThread(receiver, num);
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
