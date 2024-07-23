package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealer;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealerThread;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.TripleTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory.Z2TripleGenType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.aided.AidedZ2TripleGenConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Z2 triple generation aid test.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
@RunWith(Parameterized.class)
public class Z2TripleGenAidTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2TripleGenAidTest.class);
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

        configurations.add(new Object[]{
            Z2TripleGenType.AIDED.name(), new AidedZ2TripleGenConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final Z2TripleGenConfig config;

    public Z2TripleGenAidTest(String name, Z2TripleGenConfig config) {
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
        Z2TripleGenParty sender = Z2TripleGenFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        Z2TripleGenParty receiver = Z2TripleGenFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
        TrustDealer trustDealer = new TrustDealer(thirdRpc, firstRpc.ownParty(), secondRpc.ownParty());
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        trustDealer.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        trustDealer.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Z2TripleGenPartyThread senderThread = new Z2TripleGenPartyThread(sender, num);
            Z2TripleGenPartyThread receiverThread = new Z2TripleGenPartyThread(receiver, num);
            TrustDealerThread trustDealerThread = new TrustDealerThread(trustDealer);
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            trustDealerThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            TripleTestUtils.assertOutput(num, senderThread.getFirstTriple(), receiverThread.getFirstTriple());
            TripleTestUtils.assertOutput(num, senderThread.getSecondTriple(), receiverThread.getSecondTriple());
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            trustDealerThread.join();
            new Thread(trustDealer::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
