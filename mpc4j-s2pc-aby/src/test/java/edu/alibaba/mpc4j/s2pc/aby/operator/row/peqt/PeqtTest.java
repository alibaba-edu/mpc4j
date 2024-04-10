package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory.PeqtType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22.Cgs22PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive.NaivePeqtConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * private equality test.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
@RunWith(Parameterized.class)
public class PeqtTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeqtTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * default l
     */
    private static final int DEFAULT_L = Integer.SIZE;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CGS22
        configurations.add(new Object[]{
            PeqtType.CGS22.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Cgs22PeqtConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });
        // NAIVE
        configurations.add(new Object[]{
            PeqtType.NAIVE.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new NaivePeqtConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final PeqtConfig config;

    public PeqtTest(String name, PeqtConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(DEFAULT_L, 1, false);
    }

    @Test
    public void test2Num() {
        testPto(DEFAULT_L, 2, false);
    }

    @Test
    public void test8Num() {
        testPto(DEFAULT_L, 8, false);
    }

    @Test
    public void test7Num() {
        testPto(DEFAULT_L, 7, false);
    }

    @Test
    public void test9Num() {
        testPto(DEFAULT_L, 9, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_NUM, true);
    }

    @Test
    public void test1L() {
        testPto(1, DEFAULT_NUM, false);
    }

    @Test
    public void test7L() {
        testPto(7, DEFAULT_NUM, false);
    }

    @Test
    public void test9L() {
        testPto(9, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, true);
    }

    private void testPto(int l, int num, boolean parallel) {
        // create inputs
        byte[][] xs = PeqtTestUtils.genSenderInputArray(l, num, SECURE_RANDOM);
        byte[][] ys = PeqtTestUtils.genReceiverInputArray(l, xs, SECURE_RANDOM);
        // init the protocol
        PeqtParty sender = PeqtFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PeqtParty receiver = PeqtFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PeqtPartyThread senderThread = new PeqtPartyThread(sender, l, xs);
            PeqtPartyThread receiverThread = new PeqtPartyThread(receiver, l, ys);
            StopWatch stopWatch = new StopWatch();
            // execute the protocol
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            SquareZ2Vector z0 = senderThread.getZi();
            SquareZ2Vector z1 = receiverThread.getZi();
            BitVector z = z0.getBitVector().xor(z1.getBitVector());
            assertOutput(num, xs, ys, z);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(int num, byte[][] xs, byte[][] ys, BitVector z) {
        Assert.assertEquals(num, z.bitNum());
        for (int index = 0; index < num; index++) {
            boolean xi = Arrays.equals(xs[index], ys[index]);
            if (!xi) {
                // not equal
                Assert.assertFalse(z.get(index));
            } else {
                // equal
                Assert.assertTrue(z.get(index));
            }
        }
    }
}
