package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.TripleTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.Z2TripleGenFactory.Z2TripleGenType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.direct.DirectZ2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake.FakeZ2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot.LcotZ2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.silent.SilentZ2TripleGenConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Z2 triple generation test.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
@RunWith(Parameterized.class)
public class Z2TripleGenTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2TripleGenTest.class);
    /**
     * single round num
     */
    private static final int SINGLE_ROUND_NUM = 999;
    /**
     * multiple round num
     */
    private static final int MULTIPLE_ROUND_NUM = (1 << 18) + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // silent
        configurations.add(new Object[]{
            Z2TripleGenType.SILENT_COT.name(), new SilentZ2TripleGenConfig.Builder().build(),
        });
        // direct COT
        configurations.add(new Object[]{
            Z2TripleGenType.DIRECT_COT.name(), new DirectZ2TripleGenConfig.Builder().build(),
        });
        // direct LCOT
        configurations.add(new Object[]{
            Z2TripleGenType.LCOT.name(), new LcotZ2TripleGenConfig.Builder().build(),
        });
        // fake
        configurations.add(new Object[]{
            Z2TripleGenType.FAKE.name(), new FakeZ2TripleGenConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final Z2TripleGenConfig config;

    public Z2TripleGenTest(String name, Z2TripleGenConfig config) {
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
    public void testOneRoundNum() {
        testPto(SINGLE_ROUND_NUM, false);
    }

    @Test
    public void testParallelOneRoundNum() {
        testPto(SINGLE_ROUND_NUM, true);
    }

    @Test
    public void testMultipleRoundNum() {
        testPto(MULTIPLE_ROUND_NUM, false);
    }

    @Test
    public void testParallelMultipleRound() {
        testPto(MULTIPLE_ROUND_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        Z2TripleGenParty sender = Z2TripleGenFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Z2TripleGenParty receiver = Z2TripleGenFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Z2TripleGenPartyThread senderThread = new Z2TripleGenPartyThread(sender, num);
            Z2TripleGenPartyThread receiverThread = new Z2TripleGenPartyThread(receiver, num);
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
            TripleTestUtils.assertOutput(num, senderThread.getFirstTriple(), receiverThread.getFirstTriple());
            TripleTestUtils.assertOutput(num, senderThread.getSecondTriple(), receiverThread.getSecondTriple());
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
