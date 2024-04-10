package edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.rrk20.Rrk20MillionaireConfig;
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
 * Millionaire protocol test.
 *
 * @author Li Peng
 * @date 2023/4/14
 */
@RunWith(Parameterized.class)
public class MillionaireTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(MillionaireTest.class);
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
    private static final int DEFAULT_L = 32;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRK+20
        configurations.add(new Object[]{
            MillionaireFactory.MillionaireType.RRK20 + " (" + SecurityModel.SEMI_HONEST + ")",
            new Rrk20MillionaireConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final MillionaireConfig config;

    public MillionaireTest(String name, MillionaireConfig config) {
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
    public void test4Num() {
        testPto(DEFAULT_L, 4, false);
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
    public void test19L() {
        testPto(19, DEFAULT_NUM, false);
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
        byte[][] xs = MillionaireTestUtils.genSenderInputArray(l, num, SECURE_RANDOM);
        byte[][] ys = MillionaireTestUtils.genReceiverInputArray(l, xs, SECURE_RANDOM);
        // init the protocol
        MillionaireParty sender = MillionaireFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        MillionaireParty receiver = MillionaireFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            MillionairePartyThread senderThread = new MillionairePartyThread(sender, l, xs);
            MillionairePartyThread receiverThread = new MillionairePartyThread(receiver, l, ys);
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
            boolean result = BigIntegerUtils.byteArrayToNonNegBigInteger(xs[index])
                .compareTo(BigIntegerUtils.byteArrayToNonNegBigInteger(ys[index])) < 0;
            Assert.assertEquals(z.get(index), result);
        }
    }

}

