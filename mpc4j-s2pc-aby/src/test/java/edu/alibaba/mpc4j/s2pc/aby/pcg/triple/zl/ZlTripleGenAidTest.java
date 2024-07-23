package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealer;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealerThread;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.TripleTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.ZlTripleGenFactory.ZlTripleGenType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.aided.AidedZlTripleGenConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Trust Dealer Zl triple generation test.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
@RunWith(Parameterized.class)
public class ZlTripleGenAidTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlTripleGenAidTest.class);
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
            ZlTripleGenType.AIDED.name(), new AidedZlTripleGenConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final ZlTripleGenConfig config;
    /**
     * Zl
     */
    private final Zl zl;
    /**
     * large zl
     */
    private final Zl largeZl;

    public ZlTripleGenAidTest(String name, ZlTripleGenConfig config) {
        super(name);
        this.config = config;
        zl = ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L_FOR_MODULE_N - 1);
        largeZl = ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L_FOR_MODULE_N + 1);
    }

    @Test
    public void test1Num() {
        testPto(zl, 1, false);
    }

    @Test
    public void test2Num() {
        testPto(zl, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(zl, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(zl, DEFAULT_NUM, true);
    }

    @Test
    public void testLargeZl() {
        testPto(largeZl, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeMaxL() {
        testPto(largeZl.getL(), zl, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testPto(zl, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(zl, LARGE_NUM, true);
    }

    private void testPto(Zl zl, int num, boolean parallel) {
        testPto(zl.getL(), zl, num, parallel);
    }

    private void testPto(int maxL, Zl zl, int num, boolean parallel) {
        ZlTripleGenParty sender = ZlTripleGenFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        ZlTripleGenParty receiver = ZlTripleGenFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
        TrustDealer trustDealer = new TrustDealer(thirdRpc, firstRpc.ownParty(), secondRpc.ownParty());
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        trustDealer.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlTripleGenPartyThread senderThread = new ZlTripleGenPartyThread(sender, maxL, zl, num);
            ZlTripleGenPartyThread receiverThread = new ZlTripleGenPartyThread(receiver, maxL, zl, num);
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
            TripleTestUtils.assertOutput(zl, num, senderThread.getFirstTriple(), receiverThread.getFirstTriple());
            TripleTestUtils.assertOutput(zl, num, senderThread.getSecondTriple(), receiverThread.getSecondTriple());
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
