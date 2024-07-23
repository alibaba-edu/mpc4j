package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealer;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealerThread;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.TripleTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.Zl64TripleGenFactory.Zl64TripleGenType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.aided.AidedZl64TripleGenConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Trust Dealer Zl64 triple generation test.
 *
 * @author Weiran Liu
 * @date 2024/7/1
 */
@RunWith(Parameterized.class)
public class Zl64TripleGenAidTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zl64TripleGenAidTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 99;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 13) + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Zl64TripleGenType.AIDED.name(), new AidedZl64TripleGenConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final Zl64TripleGenConfig config;
    /**
     * Zl64
     */
    private final Zl64 zl64;
    /**
     * large Zl64
     */
    private final Zl64 largeZl64;

    public Zl64TripleGenAidTest(String name, Zl64TripleGenConfig config) {
        super(name);
        this.config = config;
        zl64 = Zl64Factory.createInstance(EnvType.STANDARD, 24);
        largeZl64 = Zl64Factory.createInstance(EnvType.STANDARD, Long.SIZE);
    }

    @Test
    public void test1Num() {
        testPto(zl64, 1, false);
    }

    @Test
    public void test2Num() {
        testPto(zl64, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(zl64, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(zl64, DEFAULT_NUM, true);
    }

    @Test
    public void testLargeZl() {
        testPto(largeZl64, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeMaxL() {
        testPto(largeZl64.getL(), zl64, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testPto(zl64, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(zl64, LARGE_NUM, true);
    }

    private void testPto(Zl64 zl64, int num, boolean parallel) {
        testPto(zl64.getL(), zl64, num, parallel);
    }

    private void testPto(int maxL, Zl64 zl64, int num, boolean parallel) {
        Zl64TripleGenParty sender = Zl64TripleGenFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        Zl64TripleGenParty receiver = Zl64TripleGenFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
        TrustDealer trustDealer = new TrustDealer(thirdRpc, firstRpc.ownParty(), secondRpc.ownParty());
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        trustDealer.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Zl64TripleGenPartyThread senderThread = new Zl64TripleGenPartyThread(sender, maxL, zl64, num);
            Zl64TripleGenPartyThread receiverThread = new Zl64TripleGenPartyThread(receiver, maxL, zl64, num);
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
            TripleTestUtils.assertOutput(zl64, num, senderThread.getFirstTriple(), receiverThread.getFirstTriple());
            TripleTestUtils.assertOutput(zl64, num, senderThread.getSecondTriple(), receiverThread.getSecondTriple());
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            trustDealerThread.join();
            new Thread(trustDealer::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
