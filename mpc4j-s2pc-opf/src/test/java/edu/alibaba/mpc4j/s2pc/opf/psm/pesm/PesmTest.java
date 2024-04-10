package edu.alibaba.mpc4j.s2pc.opf.psm.pesm;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.PesmFactory.PesmType;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.cgs22.Cgs22LnotPesmConfig;
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
 * private (equal) set membership test.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
@RunWith(Parameterized.class)
public class PesmTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PesmTest.class);
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
    private static final int DEFAULT_L = 64;
    /**
     * default d
     */
    private static final int DEFAULT_D = 3;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CGS22_LNOT
        configurations.add(new Object[]{
            PesmType.CGS22_LNOT.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Cgs22LnotPesmConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final PesmConfig config;

    public PesmTest(String name, PesmConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(DEFAULT_L, DEFAULT_D, 1, false);
    }

    @Test
    public void test2Num() {
        testPto(DEFAULT_L, DEFAULT_D, 2, false);
    }

    @Test
    public void test8Num() {
        testPto(DEFAULT_L, DEFAULT_D, 8, false);
    }

    @Test
    public void test7Num() {
        testPto(DEFAULT_L, DEFAULT_D, 7, false);
    }

    @Test
    public void test9Num() {
        testPto(DEFAULT_L, DEFAULT_D, 9, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_D, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_D, DEFAULT_NUM, true);
    }

    @Test
    public void testSpecialL() {
        testPto(DEFAULT_L + 5, DEFAULT_D, DEFAULT_NUM, false);
    }

    @Test
    public void test1D() {
        testPto(DEFAULT_L, 1, DEFAULT_NUM, false);
    }

    @Test
    public void test2D() {
        testPto(DEFAULT_L, 2, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_L, DEFAULT_D, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_L, DEFAULT_D, LARGE_NUM, false);
    }

    private void testPto(int l, int d, int num, boolean parallel) {
        // create inputs
        byte[][][] senderInputArrays = PesmTestUtils.genSenderInputArrays(l, d, num, SECURE_RANDOM);
        byte[][] receiverInputArray = PesmTestUtils.genReceiverInputArray(l, d, senderInputArrays, SECURE_RANDOM);
        // init the protocol
        PesmSender sender = PesmFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PesmReceiver receiver = PesmFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PesmSenderThread senderThread = new PesmSenderThread(sender, l, d, senderInputArrays);
            PesmReceiverThread receiverThread = new PesmReceiverThread(receiver, l, d, receiverInputArray);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            SquareZ2Vector z0 = senderThread.getZ0();
            SquareZ2Vector z1 = receiverThread.getZ1();
            BitVector z = z0.getBitVector().xor(z1.getBitVector());
            // verify
            assertOutput(num, senderInputArrays, receiverInputArray, z);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int num, byte[][][] senderInputArrays, byte[][] receiverInputArray, BitVector z) {
        Assert.assertEquals(num, z.bitNum());
        for (int index = 0; index < num; index++) {
            boolean equal = false;
            for (int i = 0; i < senderInputArrays[index].length; i++) {
                equal |= Arrays.equals(senderInputArrays[index][i], receiverInputArray[index]);
            }
            if (!equal) {
                // not equal
                Assert.assertFalse(z.get(index));
            } else {
                // equal
                Assert.assertTrue(z.get(index));
            }
        }
    }
}
